package io.github.thiagojosetj.portfolio.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.thiagojosetj.portfolio.TestcontainersConfiguration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("local-wallet")
@Tag("postgres")
class IdentityServiceIntegrationTest {
  @Autowired IdentityService identities;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void cleanSyntheticFixtures() {
    jdbc.update("DELETE FROM spring_session");
    jdbc.update("DELETE FROM allocation_class");
    jdbc.update("DELETE FROM portfolio");
    jdbc.update("DELETE FROM external_identity");
    jdbc.update("DELETE FROM app_user");
  }

  @Test
  void keepsIdentityStableAcrossEmailChangesWithoutLinkingByEmail() {
    var first = identities.googleIdentity("synthetic-one", "Um fictício", "same@example.com", true);
    var changed =
        identities.googleIdentity("synthetic-one", "Um fictício", "changed@example.com", true);
    var second =
        identities.googleIdentity("synthetic-two", "Dois fictício", "same@example.com", true);
    assertThat(changed.userId()).isEqualTo(first.userId());
    assertThat(second.userId()).isNotEqualTo(first.userId());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class)).isEqualTo(2);
  }

  @Test
  void concurrentCallbacksCreateOnlyOneLocalUserAndIdentity() throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<
              io.github.thiagojosetj.portfolio.identity.security.SessionPrincipal>
          task =
              () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                  throw new IllegalStateException("Synthetic timeout");
                }
                return identities.googleIdentity(
                    "synthetic-shared", "Um fictício", "one@example.com", true);
              };
      var first = executor.submit(task);
      var second = executor.submit(task);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(first.get(10, TimeUnit.SECONDS).userId())
          .isEqualTo(second.get(10, TimeUnit.SECONDS).userId());
      assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class)).isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM external_identity", Integer.class))
          .isEqualTo(1);
    }
  }

  @Test
  void rejectsInvalidClaimsWithoutPersistingAnIdentity() {
    assertThatThrownBy(
            () ->
                identities.googleIdentity(
                    " spaced-subject ", "Fictício", "fiction@example.com", true))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class)).isZero();
  }
}
