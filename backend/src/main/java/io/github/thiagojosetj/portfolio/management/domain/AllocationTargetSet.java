package io.github.thiagojosetj.portfolio.management.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AllocationTargetSet {

  private static final int MAX_TARGETS = 20;
  private static final int MAX_NAME_LENGTH = 80;
  private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100.0000");
  private static final BigDecimal REQUIRED_TOTAL = new BigDecimal("100.0000");

  private final List<AllocationTargetDefinition> targets;

  private AllocationTargetSet(List<AllocationTargetDefinition> targets) {
    this.targets = targets;
  }

  public static AllocationTargetSet from(List<AllocationTargetDefinition> candidates) {
    if (candidates == null || candidates.isEmpty() || candidates.size() > MAX_TARGETS) {
      throw invalid("allocationTargets", "size", "Informe entre 1 e 20 classes de alocação.");
    }

    List<AllocationTargetDefinition> normalizedTargets = new ArrayList<>(candidates.size());
    Set<String> normalizedNames = new HashSet<>();
    BigDecimal total = BigDecimal.ZERO.setScale(4);

    for (int index = 0; index < candidates.size(); index++) {
      AllocationTargetDefinition candidate = candidates.get(index);
      String fieldPrefix = "allocationTargets[" + index + "]";
      if (candidate == null) {
        throw invalid(fieldPrefix, "required", "A classe de alocação é obrigatória.");
      }

      String name = normalizeName(candidate.name(), fieldPrefix + ".name");
      String nameKey = name.toLowerCase(Locale.ROOT);
      if (!normalizedNames.add(nameKey)) {
        throw invalid(
            fieldPrefix + ".name",
            "duplicate",
            "Os nomes das classes devem ser únicos na carteira.");
      }

      BigDecimal percentage =
          normalizePercentage(candidate.targetPercentage(), fieldPrefix + ".targetPercentage");
      total = total.add(percentage);
      normalizedTargets.add(new AllocationTargetDefinition(name, percentage));
    }

    if (total.compareTo(REQUIRED_TOTAL) != 0) {
      throw invalid("allocationTargets", "sum", "A soma das metas deve ser exatamente 100.0000%.");
    }

    return new AllocationTargetSet(List.copyOf(normalizedTargets));
  }

  public List<AllocationTargetDefinition> targets() {
    return targets;
  }

  private static String normalizeName(String candidate, String field) {
    if (candidate == null) {
      throw invalid(field, "required", "O nome da classe é obrigatório.");
    }

    String normalized = candidate.strip();
    if (normalized.isEmpty() || normalized.length() > MAX_NAME_LENGTH) {
      throw invalid(field, "length", "O nome da classe deve ter entre 1 e 80 caracteres.");
    }
    return normalized;
  }

  private static BigDecimal normalizePercentage(BigDecimal candidate, String field) {
    if (candidate == null) {
      throw invalid(field, "required", "A meta percentual é obrigatória.");
    }

    final BigDecimal normalized;
    try {
      normalized = candidate.setScale(4, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException exception) {
      throw invalid(field, "scale", "A meta percentual aceita no máximo quatro casas decimais.");
    }

    if (normalized.signum() < 0 || normalized.compareTo(MAX_PERCENTAGE) > 0) {
      throw invalid(field, "range", "A meta percentual deve estar entre 0.0000 e 100.0000.");
    }
    return normalized;
  }

  private static PortfolioValidationException invalid(String field, String code, String message) {
    return new PortfolioValidationException(field, code, message);
  }
}
