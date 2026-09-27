package io.github.thiagojosetj.portfolio.management.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Scalar values from one statement; a missing class is represented by a null allocationClassId. */
public record OwnedPortfolioSnapshotRow(
    UUID portfolioId,
    UUID ownerUserId,
    String portfolioName,
    String baseCurrency,
    Long portfolioVersion,
    Instant portfolioCreatedAt,
    Instant portfolioUpdatedAt,
    UUID allocationClassId,
    String allocationClassName,
    Short displayOrder,
    BigDecimal targetPercentage,
    Instant allocationClassCreatedAt,
    Instant allocationClassUpdatedAt) {}
