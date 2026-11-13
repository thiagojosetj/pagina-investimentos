package io.github.thiagojosetj.portfolio.identity.security;

import io.github.thiagojosetj.portfolio.identity.application.IdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration(proxyBeanMethods = false)
@Profile("wallet")
public class GoogleLoginConfigurer {
  private final String origin;
  private final String clientId;
  private final String clientSecret;
  private final IdentityService identity;
  private final SecurityContextRepository contexts;

  public GoogleLoginConfigurer(
      Environment environment, IdentityService identity, SecurityContextRepository contexts) {
    origin = required(environment, "portfolio.security.origin");
    URI uri = URI.create(origin);
    if (!"https".equals(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null
        || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))) {
      throw new IllegalStateException("wallet exige uma origem HTTPS canônica sem caminho.");
    }
    clientId = required(environment, "portfolio.security.google.client-id");
    clientSecret = required(environment, "portfolio.security.google.client-secret");
    this.identity = identity;
    this.contexts = contexts;
  }

  @Bean
  ClientRegistrationRepository clientRegistrationRepository() {
    String base = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
    var registration =
        ClientRegistration.withRegistrationId("google")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri(base + "/login/oauth2/code/google")
            .scope("openid", "email", "profile")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .issuerUri("https://accounts.google.com")
            .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
            .userNameAttributeName("sub")
            .clientName("Google")
            .clientSettings(
                ClientRegistration.ClientSettings.builder().requireProofKey(true).build())
            .build();
    return new InMemoryClientRegistrationRepository(registration);
  }

  void configure(HttpSecurity http) throws Exception {
    http.oauth2Login(
        login ->
            login
                .authorizedClientRepository(new DiscardingAuthorizedClientRepository())
                .successHandler(this::authenticated)
                .failureHandler(
                    (request, response, exception) ->
                        response.sendRedirect(home() + "?login=failed")));
  }

  void authenticated(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws java.io.IOException {
    try {
      if (!(authentication.getPrincipal() instanceof OidcUser user)) {
        throw new IllegalStateException("Principal OIDC esperado.");
      }
      var local =
          identity.googleIdentity(
              user.getSubject(),
              user.getFullName(),
              user.getEmail(),
              Boolean.TRUE.equals(user.getEmailVerified()));
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(
          UsernamePasswordAuthenticationToken.authenticated(
              local, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
      SecurityContextHolder.setContext(context);
      contexts.saveContext(context, request, response);
      response.sendRedirect(home() + "?login=success");
    } catch (RuntimeException exception) {
      // OAuth authentication has already saved its context; do not retain it on DB failure.
      var session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
      SecurityContextHolder.clearContext();
      response.sendRedirect(home() + "?login=failed");
    }
  }

  private String home() {
    return origin.endsWith("/") ? origin : origin + "/";
  }

  private static String required(Environment environment, String key) {
    String value = environment.getProperty(key);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("Configuração obrigatória ausente: " + key);
    }
    return value.strip();
  }

  /** Login identifies the user; the application never calls Google APIs afterwards. */
  private static final class DiscardingAuthorizedClientRepository
      implements OAuth2AuthorizedClientRepository {
    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
        String id, Authentication principal, HttpServletRequest request) {
      return null;
    }

    @Override
    public void saveAuthorizedClient(
        OAuth2AuthorizedClient client,
        Authentication principal,
        HttpServletRequest request,
        HttpServletResponse response) {}

    @Override
    public void removeAuthorizedClient(
        String id,
        Authentication principal,
        HttpServletRequest request,
        HttpServletResponse response) {}
  }
}
