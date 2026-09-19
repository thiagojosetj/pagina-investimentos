package io.github.thiagojosetj.portfolio.management.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Validates the complete replacement without assuming ownership of any referenced ID. */
public final class AllocationTargetReplacement {

  private final List<AllocationTargetUpdate> targets;

  private AllocationTargetReplacement(List<AllocationTargetUpdate> targets) {
    this.targets = List.copyOf(targets);
  }

  public static AllocationTargetReplacement from(List<AllocationTargetUpdate> candidates) {
    List<AllocationTargetDefinition> definitions =
        candidates == null
            ? null
            : candidates.stream()
                .map(
                    candidate ->
                        candidate == null
                            ? null
                            : new AllocationTargetDefinition(
                                candidate.name(), candidate.targetPercentage()))
                .toList();
    AllocationTargetSet normalized = AllocationTargetSet.from(definitions);
    Set<UUID> identifiers = new HashSet<>();
    List<AllocationTargetUpdate> targets = new ArrayList<>(candidates.size());
    for (int index = 0; index < candidates.size(); index++) {
      UUID id = candidates.get(index).id();
      if (id != null && !identifiers.add(id)) {
        throw new PortfolioValidationException(
            "allocationTargets[" + index + "].id",
            "duplicate",
            "Uma classe não pode aparecer mais de uma vez na alteração.");
      }
      AllocationTargetDefinition target = normalized.targets().get(index);
      targets.add(new AllocationTargetUpdate(id, target.name(), target.targetPercentage()));
    }
    return new AllocationTargetReplacement(targets);
  }

  public List<AllocationTargetUpdate> targets() {
    return targets;
  }
}
