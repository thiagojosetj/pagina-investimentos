package io.github.thiagojosetj.portfolio.management.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AllocationTargetReplacementTest {

  @Test
  void preservesExistingIdsAndNullNewIdsWhileNormalizingTargets() {
    UUID existingId = UUID.randomUUID();

    AllocationTargetReplacement replacement =
        AllocationTargetReplacement.from(
            List.of(
                new AllocationTargetUpdate(existingId, "  Ações  ", new BigDecimal("60")),
                new AllocationTargetUpdate(null, "  FIIs  ", new BigDecimal("40"))));

    assertThat(replacement.targets())
        .containsExactly(
            new AllocationTargetUpdate(existingId, "Ações", new BigDecimal("60.0000")),
            new AllocationTargetUpdate(null, "FIIs", new BigDecimal("40.0000")));
  }

  @Test
  void rejectsRepeatedExistingIdAtTheSecondOccurrence() {
    UUID duplicatedId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                AllocationTargetReplacement.from(
                    List.of(
                        new AllocationTargetUpdate(duplicatedId, "Ações", new BigDecimal("60")),
                        new AllocationTargetUpdate(
                            duplicatedId, "Renda fixa", new BigDecimal("40")))))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> {
              assertThat(exception.field()).isEqualTo("allocationTargets[1].id");
              assertThat(exception.code()).isEqualTo("duplicate");
            });
  }
}
