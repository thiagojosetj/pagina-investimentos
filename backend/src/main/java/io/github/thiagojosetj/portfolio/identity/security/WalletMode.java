package io.github.thiagojosetj.portfolio.identity.security;

import java.util.Arrays;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public final class WalletMode {
  private final SessionPrincipal.Origin sessionOrigin;

  public WalletMode(Environment environment) {
    var profiles = Arrays.asList(environment.getActiveProfiles());
    boolean local = profiles.contains("local-wallet");
    boolean google = profiles.contains("wallet");
    if ((local && google)
        || ((local || google) && (profiles.contains("demo") || profiles.contains("simulator")))) {
      throw new IllegalStateException("Os perfis público, local-wallet e wallet são exclusivos.");
    }
    if (local
        && !java.util.Set.of("127.0.0.1", "::1")
            .contains(environment.getProperty("server.address", "127.0.0.1"))) {
      throw new IllegalStateException("local-wallet exige bind em endereço de loopback.");
    }
    sessionOrigin =
        local ? SessionPrincipal.Origin.LOCAL : google ? SessionPrincipal.Origin.GOOGLE : null;
  }

  public String loginMode() {
    return switch (sessionOrigin) {
      case LOCAL -> "local";
      case GOOGLE -> "google";
      case null -> "demo";
    };
  }

  public boolean available() {
    return sessionOrigin != null;
  }

  boolean accepts(SessionPrincipal principal) {
    return available() && principal.origin() == sessionOrigin;
  }
}
