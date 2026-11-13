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
  private final WalletMode mode;

  AbsoluteSessionExpiryFilter(Clock clock, WalletMode mode) {
    this.clock = clock;
    this.mode = mode;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean invalidSession =
        authentication != null
            && !(authentication instanceof AnonymousAuthenticationToken)
            && (!authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SessionPrincipal user)
                || !mode.accepts(user)
                || !clock.instant().isBefore(user.authenticatedAt().plus(Duration.ofHours(8))));
    if (invalidSession) {
      var session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
      SecurityContextHolder.clearContext();
    }
    chain.doFilter(request, response);
  }
}
