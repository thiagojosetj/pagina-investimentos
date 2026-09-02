package io.github.thiagojosetj.portfolio.allocation.domain;

public class SimulationValidationException extends RuntimeException {

  private final String field;
  private final String code;

  public SimulationValidationException(String field, String code, String message) {
    super(message);
    this.field = field;
    this.code = code;
  }

  public String field() {
    return field;
  }

  public String code() {
    return code;
  }
}
