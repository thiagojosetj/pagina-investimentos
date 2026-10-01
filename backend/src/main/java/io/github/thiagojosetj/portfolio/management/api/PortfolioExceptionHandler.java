package io.github.thiagojosetj.portfolio.management.api;

import io.github.thiagojosetj.portfolio.management.application.PortfolioNotFoundException;
import io.github.thiagojosetj.portfolio.management.application.PortfolioVersionConflictException;
import io.github.thiagojosetj.portfolio.management.domain.PortfolioValidationException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PortfolioExceptionHandler {
  @ExceptionHandler(PortfolioNotFoundException.class)
  ProblemDetail missing(PortfolioNotFoundException exception) {
    return problem(
        HttpStatus.NOT_FOUND,
        "portfolio-not-found",
        "Carteira não encontrada",
        "Carteira não encontrada.");
  }

  @ExceptionHandler(PortfolioVersionConflictException.class)
  ProblemDetail conflict(PortfolioVersionConflictException exception) {
    return problem(
        HttpStatus.CONFLICT,
        "portfolio-version-conflict",
        "Carteira alterada",
        "A carteira foi alterada. Recarregue antes de salvar novamente.");
  }

  @ExceptionHandler(PortfolioValidationException.class)
  ProblemDetail validation(PortfolioValidationException exception) {
    var detail =
        problem(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "invalid-portfolio",
            "Carteira inválida",
            exception.getMessage());
    detail.setProperty("field", exception.field());
    detail.setProperty("code", exception.code());
    return detail;
  }

  private static ProblemDetail problem(HttpStatus status, String code, String title, String text) {
    var detail = ProblemDetail.forStatusAndDetail(status, text);
    detail.setType(URI.create("urn:problem:" + code));
    detail.setTitle(title);
    detail.setProperty("code", code);
    return detail;
  }
}
