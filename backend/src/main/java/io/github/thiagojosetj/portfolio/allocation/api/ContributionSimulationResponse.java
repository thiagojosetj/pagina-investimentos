package io.github.thiagojosetj.portfolio.allocation.api;

import java.util.List;

public record ContributionSimulationResponse(
    String method,
    boolean includeSales,
    String currency,
    String currentTotal,
    String contribution,
    String projectedTotal,
    List<AllocationItem> allocations,
    String disclaimer) {

  public ContributionSimulationResponse {
    allocations = List.copyOf(allocations);
  }

  public record AllocationItem(
      String classId,
      String name,
      String currentAmount,
      String currentPercentage,
      String targetPercentage,
      String targetAmount,
      String monetaryDeficit,
      String suggestedContribution,
      String suggestedPurchase,
      String suggestedSale,
      String projectedAmount,
      String projectedPercentage) {}
}
