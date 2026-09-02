package io.github.thiagojosetj.portfolio.allocation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProportionalMonetaryDeficitAllocatorTest {

  private final ProportionalMonetaryDeficitAllocator allocator =
      new ProportionalMonetaryDeficitAllocator();

  @Test
  void shouldAllocateContributionUsingMonetaryDeficitsAgainstProjectedTotal() {
    var result = allocator.allocate(examplePortfolio(), cents("2000.00"));

    assertThat(result.currentTotalInCents()).isEqualTo(cents("10000.00"));
    assertThat(result.projectedTotalInCents()).isEqualTo(cents("12000.00"));
    assertThat(contributionFor(result, "stocks")).isZero();
    assertThat(contributionFor(result, "real-estate-funds")).isEqualTo(cents("1200.00"));
    assertThat(contributionFor(result, "etfs")).isEqualTo(cents("400.00"));
    assertThat(contributionFor(result, "fixed-income")).isEqualTo(cents("400.00"));
  }

  @Test
  void shouldAllocateInsufficientContributionProportionallyAndDistributeResidualCent() {
    var result = allocator.allocate(examplePortfolio(), cents("500.00"));

    assertThat(contributionFor(result, "stocks")).isZero();
    assertThat(contributionFor(result, "real-estate-funds")).isEqualTo(cents("375.00"));
    assertThat(contributionFor(result, "etfs")).isEqualTo(cents("79.55"));
    assertThat(contributionFor(result, "fixed-income")).isEqualTo(cents("45.45"));
    assertThat(totalSuggested(result)).isEqualTo(cents("500.00"));
  }

  @Test
  void shouldUseTargetDistributionForAnEmptyPortfolio() {
    var result =
        allocator.allocate(
            List.of(
                allocation("stocks", "Ações", "0.00", "40.0000"),
                allocation("funds", "FIIs", "0.00", "25.0000"),
                allocation("etfs", "ETFs", "0.00", "15.0000"),
                allocation("fixed", "Renda fixa", "0.00", "20.0000")),
            cents("100.00"));

    assertThat(contributionFor(result, "stocks")).isEqualTo(cents("40.00"));
    assertThat(contributionFor(result, "funds")).isEqualTo(cents("25.00"));
    assertThat(contributionFor(result, "etfs")).isEqualTo(cents("15.00"));
    assertThat(contributionFor(result, "fixed")).isEqualTo(cents("20.00"));
  }

  @Test
  void shouldResolveExactResidualCentTiesByStableClassId() {
    var result =
        allocator.allocate(
            List.of(
                allocation("z-class", "Classe Z", "0.00", "50.0000"),
                allocation("a-class", "Classe A", "0.00", "50.0000")),
            cents("0.01"));

    assertThat(contributionFor(result, "a-class")).isEqualTo(cents("0.01"));
    assertThat(contributionFor(result, "z-class")).isZero();
  }

  @Test
  void shouldRejectTargetsThatDoNotSumExactlyOneHundredPercent() {
    var invalid =
        List.of(
            allocation("stocks", "Ações", "100.00", "60.0000"),
            allocation("funds", "FIIs", "100.00", "30.0000"));

    assertThatThrownBy(() -> allocator.allocate(invalid, cents("100.00")))
        .isInstanceOf(SimulationValidationException.class)
        .hasMessage("A soma das metas deve ser exatamente 100.0000%.");
  }

  @Test
  void shouldReturnZeroSuggestionsWhenContributionIsZero() {
    var result = allocator.allocate(examplePortfolio(), BigInteger.ZERO);

    assertThat(totalSuggested(result)).isZero();
  }

  @Test
  void shouldPreserveCoreMoneyInvariantsAcrossContributionSizes() {
    var contributions =
        List.of(
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.TWO,
            BigInteger.valueOf(17),
            cents("100.00"),
            cents("499.99"),
            cents("2000.00"),
            cents("10000.00"));

    for (var contribution : contributions) {
      var result = allocator.allocate(examplePortfolio(), contribution);

      assertThat(totalSuggested(result)).isEqualTo(contribution);
      assertThat(result.projectedTotalInCents())
          .isEqualTo(result.currentTotalInCents().add(contribution));
      assertThat(result.suggestions())
          .allSatisfy(
              suggestion -> {
                assertThat(suggestion.suggestedContributionInCents()).isNotNegative();
                assertThat(suggestion.projectedAmountInCents())
                    .isEqualTo(
                        suggestion
                            .currentAmountInCents()
                            .add(suggestion.suggestedContributionInCents()));
              });
    }
  }

  @Test
  void shouldRejectNullMoneyAtTheDomainBoundary() {
    var invalid =
        List.of(new AllocationClass("stocks", "Ações", null, percentageUnits("100.0000")));

    assertThatThrownBy(() -> allocator.allocate(invalid, cents("100.00")))
        .isInstanceOf(SimulationValidationException.class)
        .hasMessage("O valor atual não pode estar ausente.");
  }

  private List<AllocationClass> examplePortfolio() {
    return List.of(
        allocation("stocks", "Ações", "4800.00", "40.0000"),
        allocation("real-estate-funds", "FIIs", "1800.00", "25.0000"),
        allocation("etfs", "ETFs", "1400.00", "15.0000"),
        allocation("fixed-income", "Renda fixa", "2000.00", "20.0000"));
  }

  private AllocationClass allocation(String id, String name, String current, String target) {
    return new AllocationClass(id, name, cents(current), percentageUnits(target));
  }

  private BigInteger contributionFor(ContributionPlan result, String classId) {
    return result.suggestions().stream()
        .filter(item -> item.classId().equals(classId))
        .findFirst()
        .orElseThrow()
        .suggestedContributionInCents();
  }

  private BigInteger totalSuggested(ContributionPlan result) {
    return result.suggestions().stream()
        .map(AllocationSuggestion::suggestedContributionInCents)
        .reduce(BigInteger.ZERO, BigInteger::add);
  }

  private BigInteger cents(String value) {
    return new java.math.BigDecimal(value).movePointRight(2).toBigIntegerExact();
  }

  private BigInteger percentageUnits(String value) {
    return new java.math.BigDecimal(value).movePointRight(4).toBigIntegerExact();
  }
}
