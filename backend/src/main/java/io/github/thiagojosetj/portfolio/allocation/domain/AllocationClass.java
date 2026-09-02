package io.github.thiagojosetj.portfolio.allocation.domain;

import java.math.BigInteger;

public record AllocationClass(
    String classId,
    String name,
    BigInteger currentAmountInCents,
    BigInteger targetPercentageUnits) {}
