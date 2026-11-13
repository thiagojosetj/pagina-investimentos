package io.github.thiagojosetj.portfolio.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thiagojosetj.portfolio.identity.api.SessionController;
import io.github.thiagojosetj.portfolio.identity.application.IdentityService;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = SessionController.class,
    properties = {
      "portfolio.security.origin=https://portfolio.example.invalid",
      "portfolio.security.google.client-id=synthetic-client",
      "portfolio.security.google.client-secret=synthetic-secret"
    })
@Import({WalletSecurityConfiguration.class, WalletMode.class, GoogleLoginConfigurer.class})
@ActiveProfiles("wallet")
class GoogleAuthorizationMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean IdentityService identities;
  @MockitoBean Clock clock;

  @Test
  void usesAFixedHttpsCallbackAndPkceDespiteSpoofedProxyHeaders() throws Exception {
    var result =
        mvc.perform(
                get("/oauth2/authorization/google")
                    .header("Host", "attacker.example.invalid")
                    .header("Forwarded", "proto=http;host=attacker.example.invalid")
                    .header("X-Forwarded-Host", "attacker.example.invalid")
                    .header("X-Forwarded-Proto", "http"))
            .andExpect(status().isFound())
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andReturn();
    var redirect = URI.create(result.getResponse().getRedirectedUrl());
    assertThat(redirect.getScheme()).isEqualTo("https");
    assertThat(redirect.getHost()).isEqualTo("accounts.google.com");
    var parameters = query(redirect);
    assertThat(parameters.get("redirect_uri"))
        .isEqualTo("https://portfolio.example.invalid/login/oauth2/code/google");
    assertThat(parameters.get("code_challenge_method")).isEqualTo("S256");
    assertThat(parameters.get("code_challenge")).hasSize(43);
    assertThat(parameters.get("state")).isNotBlank();
    assertThat(parameters.get("nonce")).isNotBlank();
    assertThat(parameters.get("scope")).contains("openid").doesNotContain("offline");
    verifyNoInteractions(identities);
  }

  @Test
  void callbackWithoutAnAttemptCannotCreateALocalIdentity() throws Exception {
    mvc.perform(
            get("/login/oauth2/code/google")
                .param("code", "synthetic-code")
                .param("state", "missing-state"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("https://portfolio.example.invalid/?login=failed"));
    verifyNoInteractions(identities);
  }

  @Test
  void callbackWithWrongStateIsRejectedBeforeAnyIdentityWrite() throws Exception {
    var start = mvc.perform(get("/oauth2/authorization/google")).andReturn();
    mvc.perform(
            get("/login/oauth2/code/google")
                .session((MockHttpSession) start.getRequest().getSession(false))
                .param("code", "synthetic-code")
                .param("state", "incorrect-state"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("https://portfolio.example.invalid/?login=failed"));
    verifyNoInteractions(identities);
  }

  @Test
  void invalidatesALocalSessionInsteadOfExposingItInGoogleMode() throws Exception {
    Instant now = Instant.parse("2026-01-01T12:00:00Z");
    when(clock.instant()).thenReturn(now);
    var session = new MockHttpSession();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            new SessionPrincipal(
                UUID.randomUUID(), "Investidor fictício", now, SessionPrincipal.Origin.LOCAL),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    mvc.perform(get("/api/v1/session").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false))
        .andExpect(jsonPath("$.loginMode").value("google"));
    assertThat(session.isInvalid()).isTrue();
    verifyNoInteractions(identities);
  }

  private Map<String, String> query(URI uri) {
    return Arrays.stream(uri.getRawQuery().split("&"))
        .map(item -> item.split("=", 2))
        .collect(
            Collectors.toMap(
                pair -> URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
  }
}
