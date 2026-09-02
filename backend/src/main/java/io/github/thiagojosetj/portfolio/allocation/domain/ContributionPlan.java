package io.github.thiagojosetj.portfolio.allocation.domain;

import java.math.BigInteger;
import java.util.List;

public record ContributionPlan(
    BigInteger currentTotalInCents,
    BigInteger contributionInCents,
    BigInteger projectedTotalInCents,
    List<AllocationSuggestion> suggestions) {

  public ContributionPlan {
    suggestions = List.copyOf(suggestions);
  }
}
