package io.github.thiagojosetj.portfolio.shared.api;

import io.github.thiagojosetj.portfolio.allocation.domain.SimulationValidationException;
import java.net.URI;
import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(SimulationValidationException.class)
  ProblemDetail handleSimulationValidation(SimulationValidationException exception) {
    var problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    problem.setType(URI.create("urn:problem:invalid-simulation"));
    problem.setTitle("Simulação inválida");
    problem.setProperty("code", exception.code());
    problem.setProperty("field", exception.field());
    return problem;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleRequestValidation(MethodArgumentNotValidException exception) {
    var errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .sorted(Comparator.comparing(FieldError::field))
            .toList();
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Revise os campos obrigatórios e tente novamente.");
    problem.setType(URI.create("urn:problem:invalid-request"));
    problem.setTitle("Requisição inválida");
    problem.setProperty("code", "INVALID_REQUEST");
    problem.setProperty("errors", errors);
    return problem;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException exception) {
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Envie um corpo JSON válido conforme o contrato da API.");
    problem.setType(URI.create("urn:problem:invalid-json"));
    problem.setTitle("JSON inválido");
    problem.setProperty("code", "INVALID_JSON");
    return problem;
  }

  record FieldError(String field, String message) {}
}
