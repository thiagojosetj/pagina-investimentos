package io.github.thiagojosetj.portfolio.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.thiagojosetj.portfolio.identity.application.IdentityService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class GoogleLoginConfigurationTest {
  @Test
  void missingCredentialsFailWithoutFallback() {
    assertThatThrownBy(
            () ->
                new GoogleLoginConfigurer(
                    new MockEnvironment(),
                    mock(IdentityService.class),
                    new HttpSessionSecurityContextRepository()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("portfolio.security.origin");
  }

  @Test
  void refusesAnInsecureOrNoncanonicalOrigin() {
    for (String origin :
        List.of(
            "http://example.invalid",
            "https://example.invalid/path",
            "https://user@example.invalid",
            "https://example.invalid?redirect=evil")) {
      assertThatThrownBy(
              () ->
                  new GoogleLoginConfigurer(
                      environment(origin),
                      mock(IdentityService.class),
                      new HttpSessionSecurityContextRepository()))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Test
  void promotionFailureRemovesThePreviouslySavedOAuthContext() throws Exception {
    var service = mock(IdentityService.class);
    var repository = new HttpSessionSecurityContextRepository();
    var config =
        new GoogleLoginConfigurer(environment("https://example.invalid"), service, repository);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var session = request.getSession();
    var authentication = oidcAuthentication();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    repository.saveContext(context, request, response);
    when(service.googleIdentity(
            "synthetic-subject", "Usuário fictício", "fiction@example.com", true))
        .thenThrow(new IllegalStateException("Synthetic database unavailable"));
    try {
      config.authenticated(request, response, authentication);
      assertThat(response.getRedirectedUrl()).isEqualTo("https://example.invalid/?login=failed");
      assertThat(((org.springframework.mock.web.MockHttpSession) session).isInvalid()).isTrue();
      assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void successfulPromotionRetainsNoOidcClaimsOrTokens() throws Exception {
    var service = mock(IdentityService.class);
    var repository = new HttpSessionSecurityContextRepository();
    var config =
        new GoogleLoginConfigurer(environment("https://example.invalid"), service, repository);
    var request = new MockHttpServletRequest();
    var response = new MockHttpServletResponse();
    var principal = new SessionPrincipal(UUID.randomUUID(), "Usuário fictício", Instant.now());
    when(service.googleIdentity(
            "synthetic-subject", "Usuário fictício", "fiction@example.com", true))
        .thenReturn(principal);
    try {
      config.authenticated(request, response, oidcAuthentication());
      var context =
          (org.springframework.security.core.context.SecurityContext)
              request
                  .getSession()
                  .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
      assertThat(context.getAuthentication().getPrincipal()).isEqualTo(principal);
      assertThat(context.getAuthentication().getCredentials()).isNull();
      assertThat(response.getRedirectedUrl()).isEqualTo("https://example.invalid/");
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private MockEnvironment environment(String origin) {
    return new MockEnvironment()
        .withProperty("portfolio.security.origin", origin)
        .withProperty("portfolio.security.google.client-id", "synthetic-client")
        .withProperty("portfolio.security.google.client-secret", "synthetic-secret");
  }

  private OAuth2AuthenticationToken oidcAuthentication() {
    var token =
        new OidcIdToken(
            "synthetic-id-token",
            Instant.now(),
            Instant.now().plusSeconds(60),
            Map.of(
                "sub",
                "synthetic-subject",
                "name",
                "Usuário fictício",
                "email",
                "fiction@example.com",
                "email_verified",
                true));
    return new OAuth2AuthenticationToken(
        new DefaultOidcUser(List.of(), token), List.of(), "google");
  }
}
