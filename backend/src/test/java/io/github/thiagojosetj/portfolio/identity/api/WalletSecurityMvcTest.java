package io.github.thiagojosetj.portfolio.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thiagojosetj.portfolio.identity.application.IdentityService;
import io.github.thiagojosetj.portfolio.identity.security.SessionPrincipal;
import io.github.thiagojosetj.portfolio.identity.security.WalletMode;
import io.github.thiagojosetj.portfolio.identity.security.WalletSecurityConfiguration;
import io.github.thiagojosetj.portfolio.management.api.PortfolioController;
import io.github.thiagojosetj.portfolio.management.application.PortfolioManagementService;
import io.github.thiagojosetj.portfolio.management.application.PortfolioNotFoundException;
import io.github.thiagojosetj.portfolio.management.application.PortfolioVersionConflictException;
import io.github.thiagojosetj.portfolio.management.application.PortfolioView;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  SessionController.class,
  CsrfController.class,
  LocalSessionController.class,
  PortfolioController.class
})
@Import({WalletSecurityConfiguration.class, WalletMode.class})
@ActiveProfiles("local-wallet")
class WalletSecurityMvcTest {
  private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
  private static final UUID OWNER = UUID.fromString("2b0f8571-0b34-42c1-a654-2019142de3ce");
  private static final UUID PORTFOLIO = UUID.fromString("3b0f8571-0b34-42c1-a654-2019142de3ce");
  @Autowired MockMvc mvc;
  @MockitoBean IdentityService identities;
  @MockitoBean PortfolioManagementService portfolios;
  @MockitoBean Clock clock;

  @BeforeEach
  void configureClock() {
    when(clock.instant()).thenReturn(NOW);
  }

  @Test
  void anonymousPrivateRequestsReturnJson401WithoutRedirect() throws Exception {
    mvc.perform(get("/api/v1/portfolios"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(header().doesNotExist("Location"))
        .andExpect(header().string("Cache-Control", "private, no-store"));
    verifyNoInteractions(portfolios);
  }

  @Test
  void anonymousPortfolioWritesReturn401EvenWhenTheCsrfTokenIsAbsent() throws Exception {
    mvc.perform(
            post("/api/v1/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Carteira\",\"currency\":\"BRL\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    verifyNoInteractions(portfolios);
  }

  @Test
  void doesNotTrustAnAuthenticatedPrincipalWithoutALocalIdentity() throws Exception {
    mvc.perform(get("/api/v1/portfolios").with(user("unlinked")))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(portfolios);
  }

  @ParameterizedTest
  @NullSource
  @EnumSource(value = SessionPrincipal.Origin.class, names = "GOOGLE")
  void rejectsAndInvalidatesSessionsWithoutTheLocalOrigin(SessionPrincipal.Origin origin)
      throws Exception {
    var session = new MockHttpSession();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth(NOW, origin));
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    mvc.perform(get("/api/v1/portfolios").session(session)).andExpect(status().isUnauthorized());
    assertThat(session.isInvalid()).isTrue();
    verifyNoInteractions(portfolios);
  }

  @Test
  void exposesOnlyTheMinimalSessionState() throws Exception {
    mvc.perform(get("/api/v1/session").with(authentication(auth(NOW))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.displayName").value("Usuário fictício"))
        .andExpect(jsonPath("$.loginMode").value("local"))
        .andExpect(jsonPath("$.walletAvailable").value(true))
        .andExpect(jsonPath("$.userId").doesNotExist())
        .andExpect(jsonPath("$.origin").doesNotExist())
        .andExpect(jsonPath("$.token").doesNotExist());
  }

  @Test
  void requiresCsrfForLocalLoginAndCreatesNoIdentityOnRejection() throws Exception {
    mvc.perform(post("/api/v1/local-session"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    verifyNoInteractions(identities);
  }

  @Test
  void localLoginRotatesSessionAndStoresOnlyLocalPrincipal() throws Exception {
    when(identities.localIdentity())
        .thenReturn(
            new SessionPrincipal(OWNER, "Usuário fictício", NOW, SessionPrincipal.Origin.LOCAL));
    var session = new org.springframework.mock.web.MockHttpSession();
    String oldId = session.getId();
    mvc.perform(post("/api/v1/local-session").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(session.getId()).isNotEqualTo(oldId);
    var context =
        (org.springframework.security.core.context.SecurityContext)
            session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    assertThat(context.getAuthentication().getPrincipal()).isInstanceOf(SessionPrincipal.class);
    assertThat(context.getAuthentication().getCredentials()).isNull();
    mvc.perform(get("/api/v1/session").session(session))
        .andExpect(jsonPath("$.authenticated").value(true));
  }

  @Test
  void localLoginRejectsANonLoopbackClient() throws Exception {
    mvc.perform(
            post("/api/v1/local-session")
                .with(csrf())
                .with(
                    request -> {
                      request.setRemoteAddr("192.0.2.1");
                      return request;
                    }))
        .andExpect(status().isForbidden());
    verifyNoInteractions(identities);
  }

  @Test
  void expiresAnAuthenticatedSessionAtTheAbsoluteEightHourLimit() throws Exception {
    mvc.perform(get("/api/v1/portfolios").with(authentication(auth(NOW.minusSeconds(8 * 3600)))))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(portfolios);
  }

  @Test
  void preservesAuthenticationJustBeforeTheAbsoluteLimit() throws Exception {
    when(portfolios.listPortfolios(OWNER)).thenReturn(List.of());
    mvc.perform(
            get("/api/v1/portfolios").with(authentication(auth(NOW.minusSeconds(8 * 3600 - 1)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.portfolios").isEmpty());
    verify(portfolios).listPortfolios(OWNER);
  }

  @Test
  void requiresCsrfForPortfolioWrites() throws Exception {
    mvc.perform(
            post("/api/v1/portfolios")
                .with(authentication(auth(NOW)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Carteira\",\"currency\":\"BRL\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/portfolios")
                .with(authentication(auth(NOW)))
                .with(csrf().useInvalidToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Carteira\",\"currency\":\"BRL\"}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(portfolios);
  }

  @Test
  void derivesTheOwnerFromTheSessionAndDoesNotExposeIt() throws Exception {
    when(portfolios.createPortfolio(any())).thenReturn(view());
    mvc.perform(
            post("/api/v1/portfolios")
                .with(authentication(auth(NOW)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Carteira\",\"currency\":\"BRL\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(PORTFOLIO.toString()))
        .andExpect(jsonPath("$.ownerUserId").doesNotExist());
    var command =
        org.mockito.ArgumentCaptor.forClass(
            io.github.thiagojosetj.portfolio.management.application.CreatePortfolioCommand.class);
    verify(portfolios).createPortfolio(command.capture());
    assertThat(command.getValue().ownerUserId()).isEqualTo(OWNER);
    assertThat(command.getValue().allocationTargets()).hasSize(4);
  }

  @Test
  void rejectsForgedOwnerInput() throws Exception {
    mvc.perform(
            post("/api/v1/portfolios")
                .with(authentication(auth(NOW)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Carteira\",\"currency\":\"BRL\",\"ownerUserId\":\"00000000-0000-0000-0000-000000000000\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(portfolios);
  }

  @Test
  void convertsForeignOrMissingPortfoliosToTheSame404() throws Exception {
    when(portfolios.findPortfolio(OWNER, PORTFOLIO)).thenThrow(new PortfolioNotFoundException());
    mvc.perform(get("/api/v1/portfolios/" + PORTFOLIO).with(authentication(auth(NOW))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("portfolio-not-found"));
  }

  @Test
  void returns409WhenTheSavedVersionIsStale() throws Exception {
    when(portfolios.replaceAllocationTargets(any()))
        .thenThrow(new PortfolioVersionConflictException());
    mvc.perform(
            put("/api/v1/portfolios/" + PORTFOLIO + "/targets")
                .with(authentication(auth(NOW)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"expectedVersion\":0,\"targets\":[{\"id\":null,\"name\":\"Ações\",\"targetPercentage\":\"100.0000\",\"displayOrder\":0}]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("portfolio-version-conflict"));
  }

  @Test
  void rejectsNullTargetItemsBeforeCallingTheService() throws Exception {
    mvc.perform(
            put("/api/v1/portfolios/" + PORTFOLIO + "/targets")
                .with(authentication(auth(NOW)))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"targets\":[null]}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(portfolios);
  }

  @Test
  void csrfEndpointProvidesATokenWithoutReturningTheSessionId() throws Exception {
    mvc.perform(get("/api/v1/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.sessionId").doesNotExist());
  }

  @Test
  void logoutRequiresCsrfAndInvalidatesTheServerSession() throws Exception {
    when(identities.localIdentity())
        .thenReturn(
            new SessionPrincipal(OWNER, "Usuário fictício", NOW, SessionPrincipal.Origin.LOCAL));
    var session = new org.springframework.mock.web.MockHttpSession();
    mvc.perform(post("/api/v1/local-session").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    mvc.perform(post("/api/v1/logout").session(session)).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/logout").session(session).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(session.isInvalid()).isTrue();
  }

  private UsernamePasswordAuthenticationToken auth(Instant authenticatedAt) {
    return auth(authenticatedAt, SessionPrincipal.Origin.LOCAL);
  }

  private UsernamePasswordAuthenticationToken auth(
      Instant authenticatedAt, SessionPrincipal.Origin origin) {
    return UsernamePasswordAuthenticationToken.authenticated(
        new SessionPrincipal(OWNER, "Usuário fictício", authenticatedAt, origin),
        null,
        List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  private PortfolioView view() {
    return new PortfolioView(PORTFOLIO, OWNER, "Carteira", "BRL", 0, NOW, NOW, List.of());
  }
}
