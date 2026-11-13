package io.github.thiagojosetj.portfolio.identity.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.thiagojosetj.portfolio.TestcontainersConfiguration;
import io.github.thiagojosetj.portfolio.identity.application.IdentityService;
import io.github.thiagojosetj.portfolio.management.application.CreatePortfolioCommand;
import io.github.thiagojosetj.portfolio.management.application.PortfolioManagementService;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetDefinition;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local-wallet")
@Tag("postgres")
class WalletSessionIntegrationTest {
  @Value("${local.server.port}")
  int port;

  @Autowired JdbcTemplate jdbc;
  @Autowired JsonMapper mapper;
  @Autowired IdentityService identities;
  @Autowired PortfolioManagementService portfolios;
  HttpClient client;

  @BeforeEach
  void prepareClientAndSyntheticDatabase() {
    jdbc.update("DELETE FROM spring_session");
    jdbc.update("DELETE FROM allocation_class");
    jdbc.update("DELETE FROM portfolio");
    jdbc.update("DELETE FROM external_identity");
    jdbc.update("DELETE FROM app_user");
    client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(5))
            .build();
  }

  @Test
  void realCookieSessionPersistsMinimalIdentityAndLogsOutAcrossRequests() throws Exception {
    var initial = send("GET", "/api/v1/csrf", null, null);
    assertThat(initial.statusCode()).isEqualTo(200);
    assertThat(initial.headers().firstValue("set-cookie").orElseThrow())
        .contains("HttpOnly")
        .contains("SameSite=Lax");
    String initialId = jdbc.queryForObject("SELECT session_id FROM spring_session", String.class);
    assertThat(send("POST", "/api/v1/local-session", null, token(initial)).statusCode())
        .isEqualTo(204);
    String authenticatedId =
        jdbc.queryForObject("SELECT session_id FROM spring_session", String.class);
    assertThat(authenticatedId).isNotEqualTo(initialId);
    assertThat(
            jdbc.queryForObject("SELECT max_inactive_interval FROM spring_session", Integer.class))
        .isEqualTo(1800);
    var session = send("GET", "/api/v1/session", null, null);
    assertThat(mapper.readTree(session.body()).get("authenticated").asBoolean()).isTrue();
    byte[] stored =
        jdbc.queryForObject(
            "SELECT attribute_bytes FROM spring_session_attributes WHERE attribute_name = 'SPRING_SECURITY_CONTEXT'",
            byte[].class);
    assertThat(new String(stored, StandardCharsets.ISO_8859_1))
        .contains("SessionPrincipal")
        .doesNotContain("OidcIdToken", "OAuth2AuthenticationToken", "synthetic-id-token");
    assertThat(send("POST", "/api/v1/logout", null, null).statusCode()).isEqualTo(403);
    var csrf = send("GET", "/api/v1/csrf", null, null);
    assertThat(send("POST", "/api/v1/logout", null, token(csrf)).statusCode()).isEqualTo(204);
    assertThat(send("GET", "/api/v1/portfolios", null, null).statusCode()).isEqualTo(401);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM spring_session", Integer.class)).isZero();
  }

  @Test
  void createsReadsAndReplacesTargetsInAPersistedOwnedWallet() throws Exception {
    login();
    String csrf = token(send("GET", "/api/v1/csrf", null, null));
    var created =
        send(
            "POST",
            "/api/v1/portfolios",
            "{\"name\":\"Carteira fictícia\",\"currency\":\"BRL\"}",
            csrf);
    assertThat(created.statusCode()).isEqualTo(201);
    var wallet = mapper.readTree(created.body());
    String id = wallet.get("id").asString();
    assertThat(wallet.get("targets").size()).isEqualTo(4);
    assertThat(wallet.has("ownerUserId")).isFalse();
    assertThat(send("GET", "/api/v1/portfolios/" + id, null, null).body())
        .isEqualTo(created.body());
    assertThat(
            mapper
                .readTree(send("GET", "/api/v1/portfolios", null, null).body())
                .get("portfolios")
                .size())
        .isEqualTo(1);
    String targetId = wallet.get("targets").get(0).get("id").asString();
    String update =
        "{\"expectedVersion\":0,\"targets\":[{\"id\":\""
            + targetId
            + "\",\"name\":\"Ações\",\"targetPercentage\":\"100.0000\",\"displayOrder\":0}]}";
    var changed = send("PUT", "/api/v1/portfolios/" + id + "/targets", update, csrf);
    assertThat(changed.statusCode()).isEqualTo(200);
    assertThat(mapper.readTree(changed.body()).get("version").asLong()).isEqualTo(1);
    assertThat(mapper.readTree(changed.body()).get("targets").get(0).get("id").asString())
        .isEqualTo(targetId);
    assertThat(send("PUT", "/api/v1/portfolios/" + id + "/targets", update, csrf).statusCode())
        .isEqualTo(409);
  }

  @Test
  void neverReadsOrWritesAnotherUsersWallet() throws Exception {
    var other =
        identities.googleIdentity(
            "synthetic-other", "Outro usuário fictício", "other@example.com", true);
    var foreign =
        portfolios.createPortfolio(
            new CreatePortfolioCommand(
                other.userId(),
                "Carteira isolada",
                List.of(new AllocationTargetDefinition("ETFs", new BigDecimal("100")))));
    login();
    assertThat(send("GET", "/api/v1/portfolios/" + foreign.id(), null, null).statusCode())
        .isEqualTo(404);
    String csrf = token(send("GET", "/api/v1/csrf", null, null));
    assertThat(
            send(
                    "PUT",
                    "/api/v1/portfolios/" + foreign.id() + "/targets",
                    "{\"expectedVersion\":0,\"targets\":[{\"id\":null,\"name\":\"Ações\",\"targetPercentage\":\"100\",\"displayOrder\":0}]}",
                    csrf)
                .statusCode())
        .isEqualTo(404);
    assertThat(portfolios.findPortfolio(other.userId(), foreign.id()).version()).isZero();
  }

  @Test
  void expiredJdbcSessionsCannotUsePreviouslyIssuedCookies() throws Exception {
    login();
    assertThat(send("GET", "/api/v1/portfolios", null, null).statusCode()).isEqualTo(200);
    long elapsedMillis = Duration.ofHours(1).toMillis();
    // The repository checks last access plus the inactivity timeout, not expiry_time alone.
    int updated =
        jdbc.update(
            """
            UPDATE spring_session
               SET creation_time = creation_time - ?,
                   last_access_time = last_access_time - ?,
                   expiry_time = expiry_time - ?
            """,
            elapsedMillis,
            elapsedMillis,
            elapsedMillis);
    assertThat(updated).isOne();
    assertThat(send("GET", "/api/v1/portfolios", null, null).statusCode()).isEqualTo(401);
  }

  private void login() throws Exception {
    var csrf = send("GET", "/api/v1/csrf", null, null);
    assertThat(send("POST", "/api/v1/local-session", null, token(csrf)).statusCode())
        .isEqualTo(204);
  }

  private String token(HttpResponse<String> response) {
    return mapper.readTree(response.body()).get("token").asString();
  }

  private HttpResponse<String> send(String method, String path, String body, String csrf)
      throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body));
    if (csrf != null) {
      request.header("X-CSRF-TOKEN", csrf);
    }
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
