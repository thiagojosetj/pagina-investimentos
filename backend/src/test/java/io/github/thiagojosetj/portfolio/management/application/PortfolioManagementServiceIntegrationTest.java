package io.github.thiagojosetj.portfolio.management.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import io.github.thiagojosetj.portfolio.TestcontainersConfiguration;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetDefinition;
import io.github.thiagojosetj.portfolio.management.domain.PortfolioValidationException;
import io.github.thiagojosetj.portfolio.management.persistence.AllocationClassJpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PortfolioManagementServiceIntegrationTest {

  @Autowired PortfolioManagementService service;

  @Autowired JdbcTemplate jdbcTemplate;

  @MockitoSpyBean AllocationClassJpaRepository allocationClassRepository;

  @BeforeEach
  void cleanSyntheticFixtures() {
    reset(allocationClassRepository);
    jdbcTemplate.update("DELETE FROM allocation_class");
    jdbcTemplate.update("DELETE FROM portfolio");
    jdbcTemplate.update("DELETE FROM external_identity");
    jdbcTemplate.update("DELETE FROM app_user");
  }

  @Test
  void createsAndReadsAnOwnedPortfolioAcrossTransactions() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");

    PortfolioView created =
        service.createPortfolio(
            new CreatePortfolioCommand(
                ownerUserId,
                "  Carteira de estudos  ",
                List.of(target("Ações", "60"), target("Renda fixa", "40"))));
    PortfolioView loaded = service.findPortfolio(ownerUserId, created.id());

    assertThat(loaded).isEqualTo(created);
    assertThat(loaded.name()).isEqualTo("Carteira de estudos");
    assertThat(loaded.baseCurrency()).isEqualTo("BRL");
    assertThat(loaded.version()).isZero();
    assertThat(loaded.createdAt()).isNotNull();
    assertThat(loaded.updatedAt()).isEqualTo(loaded.createdAt());
    assertThat(loaded.allocationTargets())
        .extracting(
            PortfolioView.AllocationTargetView::name,
            PortfolioView.AllocationTargetView::displayOrder,
            PortfolioView.AllocationTargetView::targetPercentage)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Ações", 0, new BigDecimal("60.0000")),
            org.assertj.core.groups.Tuple.tuple("Renda fixa", 1, new BigDecimal("40.0000")));
  }

  @Test
  void translatesThePortfolioNameConstraintIntoAValidationError() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    createBalancedPortfolio(ownerUserId);

    assertThatThrownBy(
            () ->
                service.createPortfolio(
                    new CreatePortfolioCommand(
                        ownerUserId,
                        "carteira sintética",
                        List.of(target("Ações", "60"), target("Renda fixa", "40")))))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> {
              assertThat(exception.field()).isEqualTo("name");
              assertThat(exception.code()).isEqualTo("duplicate");
            });
  }

  @Test
  void replacesTheWholeTargetSetAndIncrementsThePortfolioVersionOnce() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView created = createBalancedPortfolio(ownerUserId);
    Set<UUID> previousTargetIds =
        created.allocationTargets().stream()
            .map(PortfolioView.AllocationTargetView::id)
            .collect(java.util.stream.Collectors.toSet());

    PortfolioView replaced =
        service.replaceAllocationTargets(
            new ReplaceAllocationTargetsCommand(
                ownerUserId,
                created.id(),
                created.version(),
                List.of(target("Ações", "45"), target("FIIs", "25"), target("Renda fixa", "30"))));

    assertThat(replaced.version()).isEqualTo(created.version() + 1);
    assertThat(replaced.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    assertThat(replaced.allocationTargets())
        .extracting(PortfolioView.AllocationTargetView::name)
        .containsExactly("Ações", "FIIs", "Renda fixa");
    assertThat(replaced.allocationTargets())
        .extracting(PortfolioView.AllocationTargetView::displayOrder)
        .containsExactly(0, 1, 2);
    assertThat(replaced.allocationTargets())
        .extracting(PortfolioView.AllocationTargetView::id)
        .doesNotContainAnyElementsOf(previousTargetIds);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT sum(target_percentage) FROM allocation_class WHERE portfolio_id = ?",
                BigDecimal.class,
                created.id()))
        .isEqualByComparingTo("100.0000");
  }

  @Test
  void invalidTargetsLeaveThePreviousAggregateUntouched() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView before = createBalancedPortfolio(ownerUserId);

    assertThatThrownBy(
            () ->
                service.replaceAllocationTargets(
                    new ReplaceAllocationTargetsCommand(
                        ownerUserId,
                        before.id(),
                        before.version(),
                        List.of(target("Ações", "50"), target("Renda fixa", "49.9999")))))
        .isInstanceOf(PortfolioValidationException.class);

    assertThat(service.findPortfolio(ownerUserId, before.id())).isEqualTo(before);
  }

  @Test
  void rollsBackTheClaimAndDeleteWhenPersistingTheReplacementFails() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView before = createBalancedPortfolio(ownerUserId);
    doThrow(new IllegalStateException("Falha sintética após a exclusão das metas."))
        .when(allocationClassRepository)
        .saveAll(any());

    try {
      assertThatThrownBy(
              () ->
                  service.replaceAllocationTargets(
                      new ReplaceAllocationTargetsCommand(
                          ownerUserId,
                          before.id(),
                          before.version(),
                          List.of(target("Ações", "70"), target("Renda fixa", "30")))))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Falha sintética após a exclusão das metas.");
    } finally {
      reset(allocationClassRepository);
    }

    assertThat(service.findPortfolio(ownerUserId, before.id())).isEqualTo(before);
  }

  @Test
  void rejectsAStaleVersionWithoutChangingTheWinningTargets() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView created = createBalancedPortfolio(ownerUserId);
    PortfolioView winningUpdate =
        service.replaceAllocationTargets(
            new ReplaceAllocationTargetsCommand(
                ownerUserId,
                created.id(),
                created.version(),
                List.of(target("Ações", "70"), target("Renda fixa", "30"))));

    assertThatThrownBy(
            () ->
                service.replaceAllocationTargets(
                    new ReplaceAllocationTargetsCommand(
                        ownerUserId,
                        created.id(),
                        created.version(),
                        List.of(target("FIIs", "50"), target("ETFs", "50")))))
        .isInstanceOf(PortfolioVersionConflictException.class);

    assertThat(service.findPortfolio(ownerUserId, created.id())).isEqualTo(winningUpdate);
  }

  @Test
  void hidesThePortfolioFromAnotherOwnerForReadsAndWrites() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético Um");
    UUID otherUserId = insertSyntheticUser("Usuário Sintético Dois");
    PortfolioView created = createBalancedPortfolio(ownerUserId);

    assertThatThrownBy(() -> service.findPortfolio(otherUserId, created.id()))
        .isInstanceOf(PortfolioNotFoundException.class)
        .hasMessage("Carteira não encontrada.");
    assertThatThrownBy(
            () ->
                service.replaceAllocationTargets(
                    new ReplaceAllocationTargetsCommand(
                        otherUserId,
                        created.id(),
                        created.version(),
                        List.of(target("FIIs", "50"), target("ETFs", "50")))))
        .isInstanceOf(PortfolioNotFoundException.class)
        .hasMessage("Carteira não encontrada.");

    assertThat(service.findPortfolio(ownerUserId, created.id())).isEqualTo(created);
  }

  @Test
  void allowsOnlyOneReplacementForTheSameVersionWithoutMixingTargetSets() throws Exception {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView created = createBalancedPortfolio(ownerUserId);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    Callable<Object> first =
        replacementTask(
            ready,
            start,
            new ReplaceAllocationTargetsCommand(
                ownerUserId,
                created.id(),
                created.version(),
                List.of(target("Ações", "75"), target("FIIs", "25"))));
    Callable<Object> second =
        replacementTask(
            ready,
            start,
            new ReplaceAllocationTargetsCommand(
                ownerUserId,
                created.id(),
                created.version(),
                List.of(target("ETFs", "35"), target("Renda fixa", "65"))));

    try (var executor = Executors.newFixedThreadPool(2)) {
      var firstResult = executor.submit(first);
      var secondResult = executor.submit(second);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      List<Object> outcomes =
          List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
      assertThat(outcomes).filteredOn(PortfolioView.class::isInstance).hasSize(1);
      assertThat(outcomes)
          .filteredOn(PortfolioVersionConflictException.class::isInstance)
          .hasSize(1);
    }

    PortfolioView stored = service.findPortfolio(ownerUserId, created.id());
    assertThat(stored.version()).isEqualTo(created.version() + 1);
    List<String> storedNames =
        stored.allocationTargets().stream().map(PortfolioView.AllocationTargetView::name).toList();
    assertThat(Set.of(List.of("Ações", "FIIs"), List.of("ETFs", "Renda fixa")))
        .contains(storedNames);
  }

  private Callable<Object> replacementTask(
      CountDownLatch ready, CountDownLatch start, ReplaceAllocationTargetsCommand command) {
    return () -> {
      ready.countDown();
      if (!start.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("A execução concorrente não foi liberada.");
      }
      try {
        return service.replaceAllocationTargets(command);
      } catch (PortfolioVersionConflictException exception) {
        return exception;
      }
    };
  }

  private PortfolioView createBalancedPortfolio(UUID ownerUserId) {
    return service.createPortfolio(
        new CreatePortfolioCommand(
            ownerUserId,
            "Carteira Sintética",
            List.of(target("Ações", "60"), target("Renda fixa", "40"))));
  }

  private UUID insertSyntheticUser(String displayName) {
    UUID userId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO app_user (id, display_name) VALUES (?, ?)", userId, displayName);
    return userId;
  }

  private static AllocationTargetDefinition target(String name, String percentage) {
    return new AllocationTargetDefinition(name, new BigDecimal(percentage));
  }
}
