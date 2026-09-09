package io.github.thiagojosetj.portfolio.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestSizeLimitFilterTest {

  private static final long LIMIT = 1024;

  private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(LIMIT);

  @Test
  void deixaPassarCorpoDentroDoLimite() throws Exception {
    var request = postWithBodyOfSize((int) LIMIT);
    var response = new MockHttpServletResponse();
    FilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEmpty();
  }

  @Test
  void recusaCorpoAcimaDoLimiteComProblemJson() throws Exception {
    var request = postWithBodyOfSize((int) LIMIT + 1);
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getContentAsString()).contains("urn:problem:payload-too-large");
    assertThat(chain.getRequest()).as("a requisição não deve seguir para a aplicação").isNull();
  }

  @Test
  void deixaPassarRequisicaoSemCorpo() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/system/status");
    var response = new MockHttpServletResponse();
    FilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
  }

  private static MockHttpServletRequest postWithBodyOfSize(int size) {
    var request =
        new MockHttpServletRequest("POST", "/api/v1/allocation-simulations/contributions");
    request.setContentType("application/json");
    request.setContent(new byte[size]);
    return request;
  }
}
