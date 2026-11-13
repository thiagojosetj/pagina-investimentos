package io.github.thiagojosetj.portfolio.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PrivateResponseHeadersFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (path.startsWith("/api/v1/portfolios")
        || path.equals("/api/v1/session")
        || path.equals("/api/v1/csrf")
        || path.equals("/api/v1/local-session")
        || path.equals("/api/v1/logout")) {
      response.setHeader("Cache-Control", "private, no-store");
    }
    chain.doFilter(request, response);
  }
}
