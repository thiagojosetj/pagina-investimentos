package io.github.thiagojosetj.portfolio.management.application;

import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetDefinition;
import java.util.List;
import java.util.UUID;

public record CreatePortfolioCommand(
    UUID ownerUserId, String name, List<AllocationTargetDefinition> allocationTargets) {}
