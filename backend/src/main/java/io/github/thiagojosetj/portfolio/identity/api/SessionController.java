package io.github.thiagojosetj.portfolio.identity.api;

import io.github.thiagojosetj.portfolio.identity.security.SessionPrincipal;
import io.github.thiagojosetj.portfolio.identity.security.WalletMode;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {
  private final WalletMode mode;

  public SessionController(WalletMode mode) {
    this.mode = mode;
  }

  @GetMapping("/api/v1/session")
  public SessionResponse session(Authentication authentication) {
    SessionPrincipal principal =
        mode.available()
                && authentication != null
                && authentication.getPrincipal() instanceof SessionPrincipal user
            ? user
            : null;
    return new SessionResponse(
        principal != null,
        principal == null ? null : principal.displayName(),
        mode.loginMode(),
        mode.available());
  }

  public record SessionResponse(
      boolean authenticated, String displayName, String loginMode, boolean walletAvailable) {}
}
