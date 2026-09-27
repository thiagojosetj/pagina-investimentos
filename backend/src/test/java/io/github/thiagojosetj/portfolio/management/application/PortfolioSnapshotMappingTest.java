package io.github.thiagojosetj.portfolio.management.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thiagojosetj.portfolio.management.domain.PortfolioValidationException;
import io.github.thiagojosetj.portfolio.management.persistence.AllocationClassJpaRepository;
import io.github.thiagojosetj.portfolio.management.persistence.OwnedPortfolioSnapshotRow;
import io.github.thiagojosetj.portfolio.management.persistence.PortfolioJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortfolioSnapshotMappingTest {

  private final PortfolioJpaRepository portfolios = mock(PortfolioJpaRepository.class);
  private final AllocationClassJpaRepository targets = mock(AllocationClassJpaRepository.class);
  private final PortfolioManagementService service =
      new PortfolioManagementService(portfolios, targets, Clock.systemUTC());
  private final UUID ownerId = UUID.randomUUID();
  private final UUID portfolioId = UUID.randomUUID();
  private final Instant now = Instant.parse("2026-09-27T12:00:00Z");

  @Test
  void mapsOnlySnapshotValuesWithoutLoadingManagedTargets() {
    UUID firstId = UUID.randomUUID();
    UUID secondId = UUID.randomUUID();
    List<OwnedPortfolioSnapshotRow> rows =
        new ArrayList<>(
            List.of(
                row(firstId, "Ações", (short) 0, "60"), row(secondId, "FIIs", (short) 1, "40")));
    when(portfolios.findOwnedSnapshotRows(ownerId, portfolioId)).thenReturn(rows);

    PortfolioView result = service.findPortfolio(ownerId, portfolioId);
    rows.clear();

    assertThat(result.id()).isEqualTo(portfolioId);
    assertThat(result.ownerUserId()).isEqualTo(ownerId);
    assertThat(result.name()).isEqualTo("Carteira Sintética");
    assertThat(result.baseCurrency()).isEqualTo("BRL");
    assertThat(result.version()).isEqualTo(7);
    assertThat(result.createdAt()).isEqualTo(now);
    assertThat(result.updatedAt()).isEqualTo(now);
    assertThat(result.allocationTargets())
        .extracting(PortfolioView.AllocationTargetView::id)
        .containsExactly(firstId, secondId);
    assertThat(result.allocationTargets().getFirst().name()).isEqualTo("Ações");
    assertThat(result.allocationTargets().getFirst().displayOrder()).isZero();
    assertThat(result.allocationTargets().getFirst().targetPercentage()).isEqualByComparingTo("60");
    assertThat(result.allocationTargets().getFirst().createdAt()).isEqualTo(now);
    assertThat(result.allocationTargets().getFirst().updatedAt()).isEqualTo(now);
    assertThatThrownBy(() -> result.allocationTargets().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    verify(portfolios).findOwnedSnapshotRows(ownerId, portfolioId);
    verifyNoInteractions(targets);
  }

  @Test
  void mapsTheLeftJoinSentinelToAnEmptyTargetList() {
    when(portfolios.findOwnedSnapshotRows(ownerId, portfolioId))
        .thenReturn(List.of(row(null, null, null, null)));

    PortfolioView result = service.findPortfolio(ownerId, portfolioId);

    assertThat(result.id()).isEqualTo(portfolioId);
    assertThat(result.version()).isEqualTo(7);
    assertThat(result.allocationTargets()).isEmpty();
    verifyNoInteractions(targets);
  }

  @Test
  void hidesAMissingOrForeignPortfolioWhenTheSnapshotIsEmpty() {
    when(portfolios.findOwnedSnapshotRows(ownerId, portfolioId)).thenReturn(List.of());

    assertThatThrownBy(() -> service.findPortfolio(ownerId, portfolioId))
        .isInstanceOf(PortfolioNotFoundException.class)
        .hasMessage("Carteira não encontrada.");
    verifyNoInteractions(targets);
  }

  @Test
  void rejectsMissingIdentifiersBeforeQuerying() {
    assertThatThrownBy(() -> service.findPortfolio(null, portfolioId))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> assertThat(exception.field()).isEqualTo("ownerUserId"));
    assertThatThrownBy(() -> service.findPortfolio(ownerId, null))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> assertThat(exception.field()).isEqualTo("portfolioId"));
    verifyNoInteractions(portfolios, targets);
  }

  private OwnedPortfolioSnapshotRow row(UUID classId, String name, Short order, String percentage) {
    return new OwnedPortfolioSnapshotRow(
        portfolioId,
        ownerId,
        "Carteira Sintética",
        "BRL",
        7L,
        now,
        now,
        classId,
        name,
        order,
        percentage == null ? null : new BigDecimal(percentage),
        classId == null ? null : now,
        classId == null ? null : now);
  }
}
