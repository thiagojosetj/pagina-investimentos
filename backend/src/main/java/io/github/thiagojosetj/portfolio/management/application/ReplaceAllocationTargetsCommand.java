package io.github.thiagojosetj.portfolio.management.application;

import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetUpdate;
import java.util.List;
import java.util.UUID;

public record ReplaceAllocationTargetsCommand(
    UUID ownerUserId,
    UUID portfolioId,
    long expectedPortfolioVersion,
    List<AllocationTargetUpdate> allocationTargets) {}
