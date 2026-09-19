package io.github.thiagojosetj.portfolio.management.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import io.github.thiagojosetj.portfolio.TestcontainersConfiguration;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetDefinition;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetUpdate;
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
  void retainsExistingIdsAndTimestampsWhileAddingAndRemovingTargets() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView created = createBalancedPortfolio(ownerUserId);
    PortfolioView.AllocationTargetView shares = created.allocationTargets().get(0);
    PortfolioView.AllocationTargetView fixedIncome = created.allocationTargets().get(1);

    PortfolioView replaced =
        service.replaceAllocationTargets(
            new ReplaceAllocationTargetsCommand(
                ownerUserId,
                created.id(),
                created.version(),
                List.of(
                    update(shares.id(), "Ações brasileiras", "45"),
                    newTarget("FIIs", "25"),
                    newTarget("ETFs", "30"))));

    assertThat(replaced.version()).isEqualTo(created.version() + 1);
    assertThat(replaced.updatedAt()).isAfterOrEqualTo(created.updatedAt());
    assertThat(replaced.allocationTargets())
        .extracting(PortfolioView.AllocationTargetView::name)
        .containsExactly("Ações brasileiras", "FIIs", "ETFs");
    assertThat(replaced.allocationTargets())
        .extracting(PortfolioView.AllocationTargetView::displayOrder)
        .containsExactly(0, 1, 2);
    assertThat(replaced.allocationTargets().get(0).id()).isEqualTo(shares.id());
    assertThat(replaced.allocationTargets().get(0).createdAt()).isEqualTo(shares.createdAt());
    assertThat(replaced.allocationTargets().get(1).id()).isNotIn(shares.id(), fixedIncome.id());
    assertThat(replaced.allocationTargets().get(2).id()).isNotIn(shares.id(), fixedIncome.id());
    assertThat(replaced.allocationTargets().get(1).id())
        .isNotEqualTo(replaced.allocationTargets().get(2).id());
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM allocation_class WHERE id = ?", Long.class, fixedIncome.id()))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT sum(target_percentage) FROM allocation_class WHERE portfolio_id = ?",
                BigDecimal.class,
                created.id()))
        .isEqualByComparingTo("100.0000");
  }

  @Test
  void swapsNamesAndOrdersWithoutRecreatingEitherClass() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView before = createBalancedPortfolio(ownerUserId);
    PortfolioView.AllocationTargetView shares = before.allocationTargets().get(0);
    PortfolioView.AllocationTargetView fixedIncome = before.allocationTargets().get(1);

    PortfolioView after =
        service.replaceAllocationTargets(
            new ReplaceAllocationTargetsCommand(
                ownerUserId,
                before.id(),
                before.version(),
                List.of(
                    update(fixedIncome.id(), "Ações", "55"),
                    update(shares.id(), "Renda fixa", "45"))));

    assertThat(after.allocationTargets())
        .extracting(
            PortfolioView.AllocationTargetView::id,
            PortfolioView.AllocationTargetView::name,
            PortfolioView.AllocationTargetView::displayOrder,
            PortfolioView.AllocationTargetView::targetPercentage)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(
                fixedIncome.id(), "Ações", 0, new BigDecimal("55.0000")),
            org.assertj.core.groups.Tuple.tuple(
                shares.id(), "Renda fixa", 1, new BigDecimal("45.0000")));
    assertThat(after.allocationTargets().get(0).createdAt()).isEqualTo(fixedIncome.createdAt());
    assertThat(after.allocationTargets().get(1).createdAt()).isEqualTo(shares.createdAt());
  }

  @Test
  void rotatesThreeNamesAndOrdersWithoutUniqueConstraintConflicts() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView before =
        service.createPortfolio(
            new CreatePortfolioCommand(
                ownerUserId,
                "Carteira Sintética",
                List.of(target("Ações", "40"), target("FIIs", "35"), target("ETFs", "25"))));
    PortfolioView.AllocationTargetView shares = before.allocationTargets().get(0);
    PortfolioView.AllocationTargetView funds = before.allocationTargets().get(1);
    PortfolioView.AllocationTargetView etfs = before.allocationTargets().get(2);

    PortfolioView after =
        service.replaceAllocationTargets(
            new ReplaceAllocationTargetsCommand(
                ownerUserId,
                before.id(),
                before.version(),
                List.of(
                    update(etfs.id(), "Ações", "25"),
                    update(shares.id(), "FIIs", "40"),
                    update(funds.id(), "ETFs", "35"))));

    assertThat(after.allocationTargets())
        .extracting(
            PortfolioView.AllocationTargetView::id,
            PortfolioView.AllocationTargetView::name,
            PortfolioView.AllocationTargetView::displayOrder)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(etfs.id(), "Ações", 0),
            org.assertj.core.groups.Tuple.tuple(shares.id(), "FIIs", 1),
            org.assertj.core.groups.Tuple.tuple(funds.id(), "ETFs", 2));
    assertThat(after.allocationTargets())
        .extracting(PortfolioView.AllocationTargetView::createdAt)
        .containsExactly(etfs.createdAt(), shares.createdAt(), funds.createdAt());
  }

  @Test
  void rejectsADeletedClassIdWithoutChangingThePortfolio() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView initial = createBalancedPortfolio(ownerUserId);
    UUID deletedId = initial.allocationTargets().get(0).id();
    PortfolioView afterRemoval =
        service.replaceAllocationTargets(
            new ReplaceAllocationTargetsCommand(
                ownerUserId, initial.id(), initial.version(), List.of(newTarget("FIIs", "100"))));

    assertThatThrownBy(
            () ->
                service.replaceAllocationTargets(
                    new ReplaceAllocationTargetsCommand(
                        ownerUserId,
                        initial.id(),
                        afterRemoval.version(),
                        List.of(
                            update(deletedId, "Ações", "60"),
                            update(afterRemoval.allocationTargets().get(0).id(), "FIIs", "40")))))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> {
              assertThat(exception.field()).isEqualTo("allocationTargets.id");
              assertThat(exception.code()).isEqualTo("not_found");
            });

    assertThat(service.findPortfolio(ownerUserId, initial.id())).isEqualTo(afterRemoval);
  }

  @Test
  void rejectsIdsFromAnotherPortfolioOrOwnerAndUnknownIds() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético Um");
    UUID otherUserId = insertSyntheticUser("Usuário Sintético Dois");
    PortfolioView owned = createBalancedPortfolio(ownerUserId);
    PortfolioView secondPortfolio =
        service.createPortfolio(
            new CreatePortfolioCommand(
                ownerUserId, "Outra Carteira", List.of(target("ETFs", "100"))));
    PortfolioView otherOwnersPortfolio = createBalancedPortfolio(otherUserId);

    for (UUID foreignId :
        List.of(
            secondPortfolio.allocationTargets().get(0).id(),
            otherOwnersPortfolio.allocationTargets().get(0).id(),
            UUID.randomUUID())) {
      assertThatThrownBy(
              () ->
                  service.replaceAllocationTargets(
                      new ReplaceAllocationTargetsCommand(
                          ownerUserId,
                          owned.id(),
                          owned.version(),
                          List.of(
                              update(foreignId, "Ações", "60"),
                              update(owned.allocationTargets().get(1).id(), "Renda fixa", "40")))))
          .isInstanceOfSatisfying(
              PortfolioValidationException.class,
              exception -> {
                assertThat(exception.field()).isEqualTo("allocationTargets.id");
                assertThat(exception.code()).isEqualTo("not_found");
              });
      assertThat(service.findPortfolio(ownerUserId, owned.id())).isEqualTo(owned);
    }
    assertThat(service.findPortfolio(ownerUserId, secondPortfolio.id())).isEqualTo(secondPortfolio);
    assertThat(service.findPortfolio(otherUserId, otherOwnersPortfolio.id()))
        .isEqualTo(otherOwnersPortfolio);
  }

  @Test
  void rejectsDuplicateExistingIdBeforeClaimingTheVersion() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView before = createBalancedPortfolio(ownerUserId);
    UUID duplicatedId = before.allocationTargets().get(0).id();

    assertThatThrownBy(
            () ->
                service.replaceAllocationTargets(
                    new ReplaceAllocationTargetsCommand(
                        ownerUserId,
                        before.id(),
                        before.version(),
                        List.of(
                            update(duplicatedId, "Ações", "60"),
                            update(duplicatedId, "Renda fixa", "40")))))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> {
              assertThat(exception.field()).isEqualTo("allocationTargets[1].id");
              assertThat(exception.code()).isEqualTo("duplicate");
            });

    assertThat(service.findPortfolio(ownerUserId, before.id())).isEqualTo(before);
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
                        List.of(
                            update(before.allocationTargets().get(0).id(), "Ações", "50"),
                            update(
                                before.allocationTargets().get(1).id(), "Renda fixa", "49.9999")))))
        .isInstanceOf(PortfolioValidationException.class);

    assertThat(service.findPortfolio(ownerUserId, before.id())).isEqualTo(before);
  }

  @Test
  void rollsBackTheClaimAndDeleteWhenPersistingTheReplacementFails() {
    UUID ownerUserId = insertSyntheticUser("Usuário Sintético");
    PortfolioView before = createBalancedPortfolio(ownerUserId);
    doThrow(new IllegalStateException("Falha sintética durante a alteração das metas."))
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
                          List.of(
                              update(before.allocationTargets().get(0).id(), "Renda fixa", "70"),
                              newTarget("FIIs", "30")))))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Falha sintética durante a alteração das metas.");
    } finally {
      reset(allocationClassRepository);
    }

    assertThat(service.findPortfolio(ownerUserId, before.id())).isEqualTo(before);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM allocation_class WHERE portfolio_id = ?",
                Long.class,
                before.id()))
        .isEqualTo(2L);
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
                List.of(
                    update(created.allocationTargets().get(0).id(), "Ações", "70"),
                    update(created.allocationTargets().get(1).id(), "Renda fixa", "30"))));

    assertThatThrownBy(
            () ->
                service.replaceAllocationTargets(
                    new ReplaceAllocationTargetsCommand(
                        ownerUserId,
                        created.id(),
                        created.version(),
                        List.of(newTarget("FIIs", "50"), newTarget("ETFs", "50")))))
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
                        List.of(newTarget("FIIs", "50"), newTarget("ETFs", "50")))))
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
                List.of(
                    update(created.allocationTargets().get(0).id(), "Ações", "75"),
                    newTarget("FIIs", "25"))));
    Callable<Object> second =
        replacementTask(
            ready,
            start,
            new ReplaceAllocationTargetsCommand(
                ownerUserId,
                created.id(),
                created.version(),
                List.of(
                    newTarget("ETFs", "35"),
                    update(created.allocationTargets().get(1).id(), "Renda fixa", "65"))));

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
    if (storedNames.equals(List.of("Ações", "FIIs"))) {
      assertThat(stored.allocationTargets().get(0).id())
          .isEqualTo(created.allocationTargets().get(0).id());
    } else {
      assertThat(stored.allocationTargets().get(1).id())
          .isEqualTo(created.allocationTargets().get(1).id());
    }
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

  private static AllocationTargetUpdate update(UUID id, String name, String percentage) {
    return new AllocationTargetUpdate(id, name, new BigDecimal(percentage));
  }

  private static AllocationTargetUpdate newTarget(String name, String percentage) {
    return update(null, name, percentage);
  }
}
