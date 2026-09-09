package io.github.thiagojosetj.portfolio.management.application;

public class PortfolioVersionConflictException extends RuntimeException {

  public PortfolioVersionConflictException() {
    super("A carteira foi alterada por outra operação. Atualize os dados e tente novamente.");
  }
}
