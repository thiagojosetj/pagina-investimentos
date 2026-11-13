package io.github.thiagojosetj.portfolio.identity.application;

import io.github.thiagojosetj.portfolio.identity.security.SessionPrincipal;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("wallet | local-wallet")
public class IdentityService {
  public static final UUID LOCAL_USER_ID = UUID.fromString("6f914542-b604-47de-9fd8-eab41a424420");
  public static final String LOCAL_DISPLAY_NAME = "Investidor fictício";
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public IdentityService(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Transactional
  public SessionPrincipal localIdentity() {
    jdbc.update(
        "INSERT INTO app_user(id, display_name) VALUES (?, ?) ON CONFLICT(id) DO NOTHING",
        LOCAL_USER_ID,
        LOCAL_DISPLAY_NAME);
    return new SessionPrincipal(
        LOCAL_USER_ID, LOCAL_DISPLAY_NAME, clock.instant(), SessionPrincipal.Origin.LOCAL);
  }

  @Transactional
  public SessionPrincipal googleIdentity(
      String subject, String name, String email, boolean verified) {
    if (subject == null
        || subject.isBlank()
        || subject.length() > 255
        || !subject.equals(subject.strip())
        || email == null
        || email.isBlank()
        || email.length() > 320
        || !email.equals(email.strip())) {
      throw new IllegalArgumentException("Identidade OIDC inválida.");
    }
    String displayName = name == null || name.isBlank() ? "Investidor" : name.strip();
    if (displayName.length() > 120) {
      displayName = displayName.substring(0, 120).strip();
    }
    // Serialize the same provider/sub callback, including the initial user insert.
    // Hash collisions only serialize unrelated callbacks; they cannot link their identities.
    jdbc.queryForObject(
        "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class, "GOOGLE:" + subject);
    var users =
        jdbc.queryForList(
            "SELECT user_id FROM external_identity WHERE provider = 'GOOGLE' AND subject = ?",
            UUID.class,
            subject);
    UUID userId;
    if (users.isEmpty()) {
      userId = UUID.randomUUID();
      jdbc.update("INSERT INTO app_user(id, display_name) VALUES (?, ?)", userId, displayName);
      jdbc.update(
          "INSERT INTO external_identity(id, user_id, provider, subject, email, email_verified) VALUES (?, ?, 'GOOGLE', ?, ?, ?)",
          UUID.randomUUID(),
          userId,
          subject,
          email,
          verified);
    } else {
      userId = users.getFirst();
      jdbc.update(
          "UPDATE app_user SET display_name = ?, updated_at = GREATEST(updated_at, clock_timestamp()), version = version + 1 WHERE id = ?",
          displayName,
          userId);
      jdbc.update(
          "UPDATE external_identity SET email = ?, email_verified = ?, updated_at = GREATEST(updated_at, clock_timestamp()), version = version + 1 WHERE provider = 'GOOGLE' AND subject = ?",
          email,
          verified,
          subject);
    }
    return new SessionPrincipal(
        userId, displayName, clock.instant(), SessionPrincipal.Origin.GOOGLE);
  }
}
