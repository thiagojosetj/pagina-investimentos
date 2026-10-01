package io.github.thiagojosetj.portfolio.identity.api;

import io.github.thiagojosetj.portfolio.identity.application.IdentityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("local-wallet")
public class LocalSessionController {
  private final IdentityService identity;
  private final SecurityContextRepository contextRepository;

  public LocalSessionController(
      IdentityService identity, SecurityContextRepository contextRepository) {
    this.identity = identity;
    this.contextRepository = contextRepository;
  }

  @PostMapping("/api/v1/local-session")
  public void login(HttpServletRequest request, HttpServletResponse response) {
    if (!java.util.Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1")
        .contains(request.getRemoteAddr())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    var principal = identity.localIdentity();
    var authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication, request, response);
    new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository())
        .onAuthentication(authentication, request, response);
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    contextRepository.saveContext(context, request, response);
    response.setStatus(204);
  }
}
