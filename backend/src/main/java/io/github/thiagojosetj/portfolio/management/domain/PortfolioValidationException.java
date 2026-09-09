package io.github.thiagojosetj.portfolio.management.domain;

public class PortfolioValidationException extends RuntimeException {

  private final String field;
  private final String code;

  public PortfolioValidationException(String field, String code, String message) {
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
