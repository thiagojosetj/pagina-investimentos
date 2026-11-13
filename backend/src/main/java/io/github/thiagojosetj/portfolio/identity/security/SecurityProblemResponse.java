package io.github.thiagojosetj.portfolio.identity.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class SecurityProblemResponse {
  private SecurityProblemResponse() {}

  public static void write(HttpServletResponse response, int status) throws IOException {
    response.setStatus(status);
    response.setContentType("application/problem+json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "private, no-store");
    if (status == 401) {
      response
          .getWriter()
          .write(
              "{\"type\":\"urn:problem:unauthenticated\",\"title\":\"Sessão necessária\",\"status\":401,\"detail\":\"Entre novamente para acessar sua carteira.\",\"code\":\"UNAUTHENTICATED\"}");
    } else {
      response
          .getWriter()
          .write(
              "{\"type\":\"urn:problem:forbidden\",\"title\":\"Operação não autorizada\",\"status\":403,\"detail\":\"Atualize a sessão e tente novamente.\",\"code\":\"FORBIDDEN\"}");
    }
  }
}
