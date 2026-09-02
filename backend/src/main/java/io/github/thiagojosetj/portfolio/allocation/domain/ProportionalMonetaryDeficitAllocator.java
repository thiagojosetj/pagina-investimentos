package io.github.thiagojosetj.portfolio.allocation.domain;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProportionalMonetaryDeficitAllocator {

  public static final String METHOD = "PROPORTIONAL_MONETARY_DEFICIT_V1";
  public static final BigInteger TOTAL_PERCENTAGE_UNITS = BigInteger.valueOf(1_000_000L);

  public ContributionPlan allocate(
      List<AllocationClass> allocationClasses, BigInteger contributionInCents) {
    validate(allocationClasses, contributionInCents);

    var currentTotal =
        allocationClasses.stream()
            .map(AllocationClass::currentAmountInCents)
            .reduce(BigInteger.ZERO, BigInteger::add);
    var projectedTotal = currentTotal.add(contributionInCents);

    var targetWeights =
        allocationClasses.stream()
            .map(item -> new Weight(item.classId(), item.targetPercentageUnits()))
            .toList();
    var targetAmounts = apportion(projectedTotal, targetWeights, TOTAL_PERCENTAGE_UNITS);

    var deficits = new HashMap<String, BigInteger>();
    for (var item : allocationClasses) {
      var deficit =
          targetAmounts
              .get(item.classId())
              .subtract(item.currentAmountInCents())
              .max(BigInteger.ZERO);
      deficits.put(item.classId(), deficit);
    }

    var totalDeficit = deficits.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
    Map<String, BigInteger> contributions;
    if (contributionInCents.signum() == 0) {
      contributions = zeroAmounts(allocationClasses);
    } else {
      if (totalDeficit.compareTo(contributionInCents) < 0) {
        throw new IllegalStateException("Total deficit cannot be smaller than the contribution");
      }
      var deficitWeights =
          allocationClasses.stream()
              .map(item -> new Weight(item.classId(), deficits.get(item.classId())))
              .toList();
      contributions = apportion(contributionInCents, deficitWeights, totalDeficit);
    }

    var suggestions =
        allocationClasses.stream()
            .map(
                item -> {
                  var suggestedContribution = contributions.get(item.classId());
                  return new AllocationSuggestion(
                      item.classId(),
                      item.name(),
                      item.currentAmountInCents(),
                      item.targetPercentageUnits(),
                      targetAmounts.get(item.classId()),
                      deficits.get(item.classId()),
                      suggestedContribution,
                      item.currentAmountInCents().add(suggestedContribution));
                })
            .toList();

    return new ContributionPlan(currentTotal, contributionInCents, projectedTotal, suggestions);
  }

  private void validate(List<AllocationClass> items, BigInteger contributionInCents) {
    if (items == null || items.isEmpty()) {
      throw invalid("allocations", "EMPTY_ALLOCATIONS", "Informe ao menos uma classe de ativo.");
    }
    if (items.size() > 20) {
      throw invalid(
          "allocations", "TOO_MANY_ALLOCATIONS", "Informe no máximo 20 classes de ativo.");
    }
    if (contributionInCents == null || contributionInCents.signum() < 0) {
      throw invalid("contribution", "INVALID_CONTRIBUTION", "O aporte não pode ser negativo.");
    }

    Set<String> classIds = new HashSet<>();
    var targetSum = BigInteger.ZERO;
    for (var item : items) {
      if (item == null) {
        throw invalid("allocations", "NULL_ALLOCATION", "Uma classe de ativo está ausente.");
      }
      if (item.classId() == null || item.classId().isBlank()) {
        throw invalid(
            "allocations.classId",
            "INVALID_CLASS_ID",
            "Cada classe de ativo deve possuir um identificador.");
      }
      if (item.name() == null || item.name().isBlank()) {
        throw invalid(
            "allocations.name", "INVALID_CLASS_NAME", "Informe o nome da classe de ativo.");
      }
      if (item.currentAmountInCents() == null) {
        throw invalid(
            "allocations.currentAmount",
            "INVALID_CURRENT_AMOUNT",
            "O valor atual não pode estar ausente.");
      }
      if (item.targetPercentageUnits() == null) {
        throw invalid(
            "allocations.targetPercentage",
            "INVALID_TARGET_PERCENTAGE",
            "A meta não pode estar ausente.");
      }
      if (!classIds.add(item.classId())) {
        throw invalid(
            "allocations.classId", "DUPLICATE_CLASS_ID", "Cada classe de ativo deve ser única.");
      }
      if (item.currentAmountInCents().signum() < 0) {
        throw invalid(
            "allocations.currentAmount",
            "INVALID_CURRENT_AMOUNT",
            "O valor atual não pode ser negativo.");
      }
      if (item.targetPercentageUnits().signum() < 0
          || item.targetPercentageUnits().compareTo(TOTAL_PERCENTAGE_UNITS) > 0) {
        throw invalid(
            "allocations.targetPercentage",
            "INVALID_TARGET_PERCENTAGE",
            "A meta deve estar entre 0 e 100.");
      }
      targetSum = targetSum.add(item.targetPercentageUnits());
    }

    if (!targetSum.equals(TOTAL_PERCENTAGE_UNITS)) {
      throw invalid(
          "allocations.targetPercentage",
          "INVALID_TARGET_SUM",
          "A soma das metas deve ser exatamente 100.0000%.");
    }
  }

  private Map<String, BigInteger> apportion(
      BigInteger totalInCents, List<Weight> weights, BigInteger totalWeight) {
    if (totalInCents.signum() == 0) {
      return weights.stream()
          .collect(
              HashMap::new, (map, weight) -> map.put(weight.id(), BigInteger.ZERO), Map::putAll);
    }
    if (totalWeight.signum() <= 0) {
      throw new IllegalArgumentException("Total weight must be positive");
    }

    var quotas = new ArrayList<Quota>();
    var assigned = BigInteger.ZERO;
    for (var weight : weights) {
      var division = totalInCents.multiply(weight.value()).divideAndRemainder(totalWeight);
      quotas.add(new Quota(weight.id(), division[0], division[1]));
      assigned = assigned.add(division[0]);
    }

    var remaining = totalInCents.subtract(assigned).intValueExact();
    quotas.sort(Comparator.comparing(Quota::remainder).reversed().thenComparing(Quota::id));
    for (var index = 0; index < remaining; index++) {
      var quota = quotas.get(index);
      quotas.set(index, quota.withOneAdditionalCent());
    }

    var amounts = new HashMap<String, BigInteger>();
    quotas.forEach(quota -> amounts.put(quota.id(), quota.amount()));
    return amounts;
  }

  private Map<String, BigInteger> zeroAmounts(List<AllocationClass> items) {
    var amounts = new HashMap<String, BigInteger>();
    items.forEach(item -> amounts.put(item.classId(), BigInteger.ZERO));
    return amounts;
  }

  private SimulationValidationException invalid(String field, String code, String message) {
    return new SimulationValidationException(field, code, message);
  }

  private record Weight(String id, BigInteger value) {}

  private record Quota(String id, BigInteger amount, BigInteger remainder) {
    Quota withOneAdditionalCent() {
      return new Quota(id, amount.add(BigInteger.ONE), remainder);
    }
  }
}
