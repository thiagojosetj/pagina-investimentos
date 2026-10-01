package io.github.thiagojosetj.portfolio.identity.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

final class AbsoluteSessionExpiryFilter extends OncePerRequestFilter {
  private final Clock clock;

  AbsoluteSessionExpiryFilter(Clock clock) {
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean invalidPrincipal =
        authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)
            && !(authentication.getPrincipal() instanceof SessionPrincipal);
    boolean expired =
        authentication != null
            && authentication.getPrincipal() instanceof SessionPrincipal user
            && !clock.instant().isBefore(user.authenticatedAt().plus(Duration.ofHours(8)));
    if (invalidPrincipal || expired) {
      var session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
      SecurityContextHolder.clearContext();
    }
    chain.doFilter(request, response);
  }
}
