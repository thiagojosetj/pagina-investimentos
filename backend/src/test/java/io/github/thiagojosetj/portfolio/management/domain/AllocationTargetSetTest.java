package io.github.thiagojosetj.portfolio.management.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllocationTargetSetTest {

  @Test
  void normalizesNamesAndExactPercentages() {
    AllocationTargetSet targets =
        AllocationTargetSet.from(
            List.of(target("  Ações  ", "60"), target("Renda fixa", "40.00000")));

    assertThat(targets.targets())
        .containsExactly(target("Ações", "60.0000"), target("Renda fixa", "40.0000"));
  }

  @Test
  void acceptsSixtyCharacterNamesAfterStrippingSurroundingWhitespaceWithoutTruncating() {
    String name = "A".repeat(60);

    AllocationTargetSet targets =
        AllocationTargetSet.from(List.of(target("\t " + name + " \n", "100")));

    assertThat(targets.targets()).containsExactly(target(name, "100.0000"));
  }

  @Test
  void rejectsSixtyOneCharacterNamesInsteadOfTruncating() {
    String name = "A".repeat(61);

    assertThatThrownBy(() -> AllocationTargetSet.from(List.of(target(name, "100"))))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> {
              assertThat(exception.field()).isEqualTo("allocationTargets[0].name");
              assertThat(exception.code()).isEqualTo("length");
              assertThat(exception.getMessage()).contains("1 e 60");
            });
  }

  @Test
  void measuresTheNameLimitInUtf16CharactersLikeTheSimulatorContract() {
    String name = "📈".repeat(30);

    AllocationTargetSet targets = AllocationTargetSet.from(List.of(target(name, "100")));

    assertThat(targets.targets()).containsExactly(target(name, "100.0000"));
    assertThatThrownBy(() -> AllocationTargetSet.from(List.of(target(name + "A", "100"))))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> assertThat(exception.code()).isEqualTo("length"));
  }

  @Test
  void rejectsAnInvalidNumberOfTargets() {
    assertThatThrownBy(() -> AllocationTargetSet.from(List.of()))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> {
              assertThat(exception.field()).isEqualTo("allocationTargets");
              assertThat(exception.code()).isEqualTo("size");
            });

    List<AllocationTargetDefinition> tooManyTargets = new ArrayList<>();
    for (int index = 0; index < 21; index++) {
      tooManyTargets.add(target("Classe " + index, index == 0 ? "100" : "0"));
    }
    assertThatThrownBy(() -> AllocationTargetSet.from(tooManyTargets))
        .isInstanceOf(PortfolioValidationException.class)
        .hasMessageContaining("1 e 20");
  }

  @Test
  void rejectsATotalDifferentFromOneHundredPercent() {
    assertThatThrownBy(
            () ->
                AllocationTargetSet.from(
                    List.of(target("Ações", "50"), target("Renda fixa", "49.9999"))))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> {
              assertThat(exception.field()).isEqualTo("allocationTargets");
              assertThat(exception.code()).isEqualTo("sum");
            });
  }

  @Test
  void rejectsAPercentageThatNeedsRounding() {
    assertThatThrownBy(
            () ->
                AllocationTargetSet.from(
                    List.of(target("Ações", "33.33335"), target("Renda fixa", "66.66665"))))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> assertThat(exception.code()).isEqualTo("scale"));
  }

  @Test
  void rejectsDuplicateNamesIgnoringCase() {
    assertThatThrownBy(
            () -> AllocationTargetSet.from(List.of(target("Ações", "50"), target("ações", "50"))))
        .isInstanceOfSatisfying(
            PortfolioValidationException.class,
            exception -> assertThat(exception.code()).isEqualTo("duplicate"));
  }

  private static AllocationTargetDefinition target(String name, String percentage) {
    return new AllocationTargetDefinition(name, new BigDecimal(percentage));
  }
}
