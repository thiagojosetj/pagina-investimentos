package io.github.thiagojosetj.portfolio.management.domain;

import java.math.BigDecimal;

public record AllocationTargetDefinition(String name, BigDecimal targetPercentage) {}
