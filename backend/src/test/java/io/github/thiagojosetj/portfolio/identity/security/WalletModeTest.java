package io.github.thiagojosetj.portfolio.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class WalletModeTest {
  @Test
  void disablesWalletWithoutAnExplicitPrivateProfile() {
    assertThat(new WalletMode(new MockEnvironment()).available()).isFalse();
  }

  @Test
  void refusesToCombineSyntheticLoginWithThePublicDemo() {
    var environment = new MockEnvironment();
    environment.setActiveProfiles("local-wallet", "demo");
    assertThatThrownBy(() -> new WalletMode(environment)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void refusesSimultaneousLocalAndGoogleIdentityModes() {
    var environment = new MockEnvironment();
    environment.setActiveProfiles("local-wallet", "wallet");
    assertThatThrownBy(() -> new WalletMode(environment)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void refusesASyntheticLoginListeningOutsideLoopback() {
    var environment = new MockEnvironment().withProperty("server.address", "0.0.0.0");
    environment.setActiveProfiles("local-wallet");
    assertThatThrownBy(() -> new WalletMode(environment)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void privateCookieAttributesDoNotDependOnUntrustedProxyHeaders() {
    var environment = new MockEnvironment();
    environment.setActiveProfiles("wallet");
    var serializer =
        new WalletSecurityConfiguration().cookieSerializer(new WalletMode(environment));
    var request = new org.springframework.mock.web.MockHttpServletRequest();
    var response = new org.springframework.mock.web.MockHttpServletResponse();
    serializer.writeCookieValue(
        new org.springframework.session.web.http.CookieSerializer.CookieValue(
            request, response, "synthetic-session"));
    assertThat(response.getHeader("Set-Cookie"))
        .startsWith("__Host-PORTFOLIO_SESSION=")
        .contains("Secure", "HttpOnly", "SameSite=Lax", "Path=/")
        .doesNotContain("Domain=");
  }
}
