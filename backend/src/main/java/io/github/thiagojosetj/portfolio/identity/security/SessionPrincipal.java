package io.github.thiagojosetj.portfolio.identity.security;

import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/** The session retains only local identity, never provider tokens or claims. */
public record SessionPrincipal(
    UUID userId, String displayName, Instant authenticatedAt, Origin origin)
    implements Principal, Serializable {
  public enum Origin {
    LOCAL,
    GOOGLE
  }

  @Override
  public String getName() {
    return userId.toString();
  }
}
