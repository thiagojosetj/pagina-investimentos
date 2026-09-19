package io.github.thiagojosetj.portfolio.management.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** A null ID creates a class; an existing ID keeps its identity when editing the target set. */
public record AllocationTargetUpdate(UUID id, String name, BigDecimal targetPercentage) {}
