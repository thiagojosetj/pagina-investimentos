package io.github.thiagojosetj.portfolio.identity.api;

import org.springframework.context.annotation.Profile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("wallet | local-wallet")
public class CsrfController {
  @GetMapping("/api/v1/csrf")
  public CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getHeaderName(), token.getToken());
  }

  public record CsrfResponse(String headerName, String token) {}
}
