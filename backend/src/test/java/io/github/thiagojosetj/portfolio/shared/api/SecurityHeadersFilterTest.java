package io.github.thiagojosetj.portfolio.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityHeadersFilterTest {

  @ParameterizedTest
  @ValueSource(ints = {200, 404, 422, 500})
  void preservesPolicyAcrossSuccessfulAndErrorResponses(int status) throws Exception {
    var request = new MockHttpServletRequest("GET", "/example");
    var response = new MockHttpServletResponse();

    new SecurityHeadersFilter()
        .doFilter(request, response, (incoming, outgoing) -> response.setStatus(status));

    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
    assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
    assertThat(response.getHeader("Cross-Origin-Opener-Policy")).isEqualTo("same-origin");
    assertThat(response.getHeader("Content-Security-Policy"))
        .isEqualTo(SecurityHeadersFilter.CONTENT_SECURITY_POLICY);
  }
}
