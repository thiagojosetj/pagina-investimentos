package io.github.thiagojosetj.portfolio.allocation.api;

import io.github.thiagojosetj.portfolio.allocation.domain.ContributionPlan;
import io.github.thiagojosetj.portfolio.allocation.domain.ProportionalMonetaryDeficitAllocator;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class ContributionSimulationResponseMapper {

  private static final String DISCLAIMER =
      "Simulação educacional baseada nas metas informadas. Não constitui recomendação de investimento.";

  ContributionSimulationResponse map(ContributionPlan plan) {
    var items =
        plan.suggestions().stream()
            .map(
                item ->
                    new ContributionSimulationResponse.AllocationItem(
                        item.classId(),
                        item.name(),
                        money(item.currentAmountInCents()),
                        percentage(item.currentAmountInCents(), plan.currentTotalInCents()),
                        percentageUnits(item.targetPercentageUnits()),
                        money(item.targetAmountInCents()),
                        money(item.monetaryDeficitInCents()),
                        money(item.suggestedContributionInCents()),
                        money(item.projectedAmountInCents()),
                        percentage(item.projectedAmountInCents(), plan.projectedTotalInCents())))
            .toList();

    return new ContributionSimulationResponse(
        ProportionalMonetaryDeficitAllocator.METHOD,
        "BRL",
        money(plan.currentTotalInCents()),
        money(plan.contributionInCents()),
        money(plan.projectedTotalInCents()),
        items,
        DISCLAIMER);
  }

  private String money(BigInteger cents) {
    return new BigDecimal(cents, 2).toPlainString();
  }

  private String percentageUnits(BigInteger units) {
    return new BigDecimal(units, 4).toPlainString();
  }

  private String percentage(BigInteger amount, BigInteger total) {
    if (total.signum() == 0) {
      return "0.0000";
    }
    return new BigDecimal(amount)
        .multiply(BigDecimal.valueOf(100))
        .divide(new BigDecimal(total), 4, RoundingMode.HALF_EVEN)
        .toPlainString();
  }
}
