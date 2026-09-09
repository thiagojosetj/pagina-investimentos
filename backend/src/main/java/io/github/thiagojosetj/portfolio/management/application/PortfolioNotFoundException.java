package io.github.thiagojosetj.portfolio.management.application;

public class PortfolioNotFoundException extends RuntimeException {

  public PortfolioNotFoundException() {
    super("Carteira não encontrada.");
  }
}
