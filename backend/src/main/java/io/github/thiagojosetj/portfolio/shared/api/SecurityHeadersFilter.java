package io.github.thiagojosetj.portfolio.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Política da demonstração servida na mesma origem, aplicada antes de possíveis rejeições. */
@Component
@Profile("demo")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

  static final String CONTENT_SECURITY_POLICY =
      "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';"
          + " img-src 'self' data:; connect-src 'self'; font-src 'self'; object-src 'none';"
          + " base-uri 'self'; frame-ancestors 'none'; form-action 'none'";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Referrer-Policy", "no-referrer");
    response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
    response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
    filterChain.doFilter(request, response);
  }
}
