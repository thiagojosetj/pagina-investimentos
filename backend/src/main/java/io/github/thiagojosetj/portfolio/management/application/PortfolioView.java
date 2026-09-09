package io.github.thiagojosetj.portfolio.management.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PortfolioView(
    UUID id,
    UUID ownerUserId,
    String name,
    String baseCurrency,
    long version,
    Instant createdAt,
    Instant updatedAt,
    List<AllocationTargetView> allocationTargets) {

  public PortfolioView {
    allocationTargets = List.copyOf(allocationTargets);
  }

  public record AllocationTargetView(
      UUID id,
      String name,
      int displayOrder,
      BigDecimal targetPercentage,
      Instant createdAt,
      Instant updatedAt) {}
}
