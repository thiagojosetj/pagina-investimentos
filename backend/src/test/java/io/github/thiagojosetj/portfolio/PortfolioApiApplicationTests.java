package io.github.thiagojosetj.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PortfolioApiApplicationTests {

  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void contextLoads() {}

  @Test
  void appliesTheInitialMigrationWithOnlyTheApprovedDomainTables() {
    List<String> tables =
        jdbcTemplate.queryForList(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = current_schema()
              AND table_type = 'BASE TABLE'
              AND table_name <> 'flyway_schema_history'
            ORDER BY table_name
            """,
            String.class);

    Integer appliedMigrationCount =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*)
            FROM flyway_schema_history
            WHERE version = '1' AND success = true
            """,
            Integer.class);

    assertThat(tables)
        .containsExactly("allocation_class", "app_user", "external_identity", "portfolio");
    assertThat(appliedMigrationCount).isEqualTo(1);
  }

  @Test
  @Transactional
  void rejectsTheSameExternalIdentityForDifferentUsers() {
    UUID firstUserId = UUID.randomUUID();
    UUID secondUserId = UUID.randomUUID();
    insertUser(firstUserId, "Usuário Sintético Um");
    insertUser(secondUserId, "Usuário Sintético Dois");
    insertExternalIdentity(UUID.randomUUID(), firstUserId, "synthetic-subject");

    assertThatThrownBy(
            () -> insertExternalIdentity(UUID.randomUUID(), secondUserId, "synthetic-subject"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void allowsTheSameEmailWhenProviderSubjectsAreDifferent() {
    UUID firstUserId = UUID.randomUUID();
    UUID secondUserId = UUID.randomUUID();
    insertUser(firstUserId, "Usuário Sintético Um");
    insertUser(secondUserId, "Usuário Sintético Dois");
    insertExternalIdentity(UUID.randomUUID(), firstUserId, "synthetic-subject-one");
    insertExternalIdentity(UUID.randomUUID(), secondUserId, "synthetic-subject-two");

    Integer identityCount =
        jdbcTemplate.queryForObject("SELECT count(*) FROM external_identity", Integer.class);

    assertThat(identityCount).isEqualTo(2);
  }

  @Test
  @Transactional
  void rejectsCaseInsensitiveDuplicatePortfolioNamesForTheSameOwner() {
    UUID userId = UUID.randomUUID();
    insertUser(userId, "Usuário Sintético");
    insertPortfolio(UUID.randomUUID(), userId);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO portfolio (id, owner_user_id, name, base_currency)
                    VALUES (?, ?, 'carteira sintética', 'BRL')
                    """,
                    UUID.randomUUID(),
                    userId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void rejectsAPortfolioWithAnUnsupportedBaseCurrency() {
    UUID userId = UUID.randomUUID();
    insertUser(userId, "Usuário Sintético");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO portfolio (id, owner_user_id, name, base_currency)
                    VALUES (?, ?, ?, ?)
                    """,
                    UUID.randomUUID(),
                    userId,
                    "Carteira Sintética",
                    "USD"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void rejectsAnAllocationTargetOutsideTheSupportedRange() {
    UUID userId = UUID.randomUUID();
    UUID portfolioId = UUID.randomUUID();
    insertUser(userId, "Usuário Sintético");
    insertPortfolio(portfolioId, userId);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO allocation_class
                        (id, portfolio_id, name, display_order, target_percentage)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID(),
                    portfolioId,
                    "Classe Sintética",
                    0,
                    new BigDecimal("100.0001")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void insertUser(UUID userId, String displayName) {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, display_name) VALUES (?, ?)", userId, displayName);
  }

  private void insertExternalIdentity(UUID identityId, UUID userId, String subject) {
    jdbcTemplate.update(
        """
        INSERT INTO external_identity
            (id, user_id, provider, subject, email, email_verified)
        VALUES (?, ?, 'GOOGLE', ?, 'usuario.sintetico@example.com', true)
        """,
        identityId,
        userId,
        subject);
  }

  private void insertPortfolio(UUID portfolioId, UUID ownerUserId) {
    jdbcTemplate.update(
        """
        INSERT INTO portfolio (id, owner_user_id, name, base_currency)
        VALUES (?, ?, 'Carteira Sintética', 'BRL')
        """,
        portfolioId,
        ownerUserId);
  }
}
