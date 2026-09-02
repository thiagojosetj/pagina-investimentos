package io.github.thiagojosetj.portfolio.allocation.application;

import io.github.thiagojosetj.portfolio.allocation.domain.AllocationClass;
import io.github.thiagojosetj.portfolio.allocation.domain.ContributionPlan;
import io.github.thiagojosetj.portfolio.allocation.domain.ProportionalMonetaryDeficitAllocator;
import java.math.BigInteger;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContributionSimulationService {

  private final ProportionalMonetaryDeficitAllocator allocator =
      new ProportionalMonetaryDeficitAllocator();

  public ContributionPlan simulate(
      List<AllocationClass> allocations, BigInteger contributionInCents) {
    return allocator.allocate(allocations, contributionInCents);
  }
}
