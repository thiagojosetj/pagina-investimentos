package io.github.thiagojosetj.portfolio.allocation.api;

import io.github.thiagojosetj.portfolio.allocation.domain.AllocationClass;
import io.github.thiagojosetj.portfolio.allocation.domain.SimulationValidationException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ContributionSimulationRequestMapper {

  private static final Pattern MONEY = Pattern.compile("(?:0|[1-9]\\d{0,14})(?:\\.\\d{1,2})?");
  private static final Pattern PERCENTAGE =
      Pattern.compile("(?:(?:0|[1-9]\\d?)(?:\\.\\d{1,4})?|100(?:\\.0{1,4})?)");

  MappedSimulation map(ContributionSimulationRequest request) {
    var contribution = parseMoney(request.contribution(), "contribution");
    var allocations = new ArrayList<AllocationClass>();

    for (var index = 0; index < request.allocations().size(); index++) {
      var item = request.allocations().get(index);
      var path = "allocations[" + index + "]";
      allocations.add(
          new AllocationClass(
              item.classId(),
              item.name().trim(),
              parseMoney(item.currentAmount(), path + ".currentAmount"),
              parsePercentage(item.targetPercentage(), path + ".targetPercentage")));
    }

    return new MappedSimulation(List.copyOf(allocations), contribution);
  }

  private BigInteger parseMoney(String value, String field) {
    if (value == null || !MONEY.matcher(value).matches()) {
      throw invalid(
          field,
          "INVALID_MONEY",
          "Use um valor monetário não negativo com até duas casas decimais.");
    }
    try {
      return new BigDecimal(value).setScale(2, RoundingMode.UNNECESSARY).unscaledValue();
    } catch (ArithmeticException exception) {
      throw invalid(field, "INVALID_MONEY", "O valor monetário informado é inválido.");
    }
  }

  private BigInteger parsePercentage(String value, String field) {
    if (value == null || !PERCENTAGE.matcher(value).matches()) {
      throw invalid(
          field, "INVALID_PERCENTAGE", "Use uma meta entre 0 e 100 com até quatro casas decimais.");
    }
    try {
      return new BigDecimal(value).setScale(4, RoundingMode.UNNECESSARY).unscaledValue();
    } catch (ArithmeticException exception) {
      throw invalid(field, "INVALID_PERCENTAGE", "O percentual informado é inválido.");
    }
  }

  private SimulationValidationException invalid(String field, String code, String message) {
    return new SimulationValidationException(field, code, message);
  }

  record MappedSimulation(List<AllocationClass> allocations, BigInteger contributionInCents) {}
}
