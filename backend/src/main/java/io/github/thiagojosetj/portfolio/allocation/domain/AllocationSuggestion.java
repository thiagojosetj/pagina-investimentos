package io.github.thiagojosetj.portfolio.allocation.domain;

import java.math.BigInteger;

public record AllocationSuggestion(
    String classId,
    String name,
    BigInteger currentAmountInCents,
    BigInteger targetPercentageUnits,
    BigInteger targetAmountInCents,
    BigInteger monetaryDeficitInCents,
    BigInteger suggestedContributionInCents,
    BigInteger projectedAmountInCents) {}
