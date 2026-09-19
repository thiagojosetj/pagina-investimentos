package io.github.thiagojosetj.portfolio.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestSizeLimitFilterTest {

  private static final int LIMIT = 1024;

  private final RequestSizeLimitFilter filter = new RequestSizeLimitFilter(LIMIT);

  @Test
  void shouldPreserveBodyAtTheExactDeclaredLimit() throws Exception {
    var request = postWithBodyOfSize(LIMIT);
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getContentAsString()).isEmpty();
    assertThat(chain.getRequest().getInputStream().readAllBytes()).hasSize(LIMIT);
  }

  @Test
  void shouldRejectDeclaredOversizedBodyWithoutReadingIt() throws Exception {
    var request =
        new MockHttpServletRequest("POST", "/api/v1/allocation-simulations/contributions") {
          @Override
          public long getContentLengthLong() {
            return LIMIT + 1L;
          }

          @Override
          public ServletInputStream getInputStream() {
            throw new AssertionError("An oversized declared body must not be read");
          }
        };
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getContentAsString()).contains("urn:problem:payload-too-large");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void shouldPreserveUnknownLengthBodyAtTheExactLimit() throws Exception {
    var request = unknownLengthRequest(new byte[LIMIT]);
    request.addHeader("Transfer-Encoding", "chunked");
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(chain.getRequest().getInputStream().readAllBytes()).hasSize(LIMIT);
  }

  @Test
  void shouldRejectChunkedBodyExceedingTheLimit() throws Exception {
    var request = unknownLengthRequest(new byte[LIMIT + 1]);
    request.addHeader("Transfer-Encoding", "chunked");
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentType()).startsWith("application/problem+json");
    assertThat(response.getContentAsString()).contains("urn:problem:payload-too-large");
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void shouldReadAtMostTheLimitPlusOneByte() throws Exception {
    var bytesRead = new AtomicInteger();
    var request =
        new UnknownLengthRequest() {
          @Override
          public ServletInputStream getInputStream() {
            return new ServletInputStream() {
              @Override
              public int read() {
                if (bytesRead.incrementAndGet() > LIMIT + 1) {
                  throw new AssertionError("The filter must not consume more than limit + 1 bytes");
                }
                return ' ';
              }

              @Override
              public boolean isFinished() {
                return false;
              }

              @Override
              public boolean isReady() {
                return true;
              }

              @Override
              public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException();
              }
            };
          }
        };
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(bytesRead).hasValue(LIMIT + 1);
    assertThat(chain.getRequest()).isNull();
  }

  @Test
  void shouldCountUtf8BytesRatherThanCharacters() throws Exception {
    var body = "á".repeat(LIMIT / 2 + 1);
    var request = unknownLengthRequest(body.getBytes(StandardCharsets.UTF_8));
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertThat(body.length()).isLessThan(LIMIT);
    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(chain.getRequest()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"application/json", "application/vnd.portfolio+json; charset=UTF-8"})
  void shouldProtectSupportedJsonMediaTypes(String contentType) throws Exception {
    var request = unknownLengthRequest(new byte[LIMIT + 1]);
    request.setContentType(contentType);
    var response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getStatus()).isEqualTo(413);
  }

  @Test
  void shouldReplayUtf8ReaderAndEnforceExclusiveStreamAccess() throws Exception {
    var text = "{\"name\":\"Ações\"}";
    var request = unknownLengthRequest(text.getBytes(StandardCharsets.UTF_8));
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    var chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    var wrapped = chain.getRequest();
    var reader = wrapped.getReader();
    assertThat(reader.readLine()).isEqualTo(text);
    assertThat(wrapped.getReader()).isSameAs(reader);
    assertThatIllegalStateException().isThrownBy(wrapped::getInputStream);
  }

  @Test
  void shouldReuseOneStreamAndEnforceExclusiveReaderAccess() throws Exception {
    var request = unknownLengthRequest(new byte[] {1, 2});
    var chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    var wrapped = chain.getRequest();
    var stream = wrapped.getInputStream();
    assertThat(stream.isFinished()).isFalse();
    assertThat(stream.isReady()).isTrue();
    assertThat(stream.read()).isEqualTo(1);
    assertThat(wrapped.getInputStream()).isSameAs(stream);
    assertThat(stream.readAllBytes()).containsExactly((byte) 2);
    assertThat(stream.isFinished()).isTrue();
    assertThat(stream.read()).isEqualTo(-1);
    assertThatIllegalStateException().isThrownBy(wrapped::getReader);
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
  void shouldNotConsumeUnrelatedReadOnlyRequests(String method) throws Exception {
    var request = unknownLengthRequest(new byte[] {1, 2});
    request.setMethod(method);
    var chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isSameAs(request);
    assertThat(request.getInputStream().readAllBytes()).containsExactly((byte) 1, (byte) 2);
  }

  @ParameterizedTest
  @ValueSource(strings = {"application/x-www-form-urlencoded", "multipart/form-data", "invalid"})
  void shouldLeaveNonJsonParsingToTheServletContainer(String contentType) throws Exception {
    var request = unknownLengthRequest(new byte[] {1, 2});
    request.setContentType(contentType);
    request.addParameter("example", "value");
    var chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isSameAs(request);
    assertThat(request.getParameter("example")).isEqualTo("value");
    assertThat(request.getInputStream().readAllBytes()).containsExactly((byte) 1, (byte) 2);
  }

  @Test
  void shouldMakeTheSynchronousBodyContractExplicit() throws Exception {
    var request = unknownLengthRequest(new byte[] {1});
    request.setAsyncSupported(true);
    var chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    var wrapped = (HttpServletRequest) chain.getRequest();
    assertThat(wrapped.isAsyncSupported()).isFalse();
    assertThatIllegalStateException().isThrownBy(wrapped::startAsync);
    assertThatIllegalStateException()
        .isThrownBy(() -> wrapped.startAsync(wrapped, new MockHttpServletResponse()));
    assertThatIllegalStateException()
        .isThrownBy(
            () ->
                wrapped
                    .getInputStream()
                    .setReadListener(
                        new ReadListener() {
                          @Override
                          public void onDataAvailable() {}

                          @Override
                          public void onAllDataRead() {}

                          @Override
                          public void onError(Throwable failure) {}
                        }));
  }

  @ParameterizedTest
  @ValueSource(longs = {0, -1, Integer.MAX_VALUE, Long.MAX_VALUE})
  void shouldRejectInvalidConfiguredLimits(long invalidLimit) {
    assertThatIllegalArgumentException().isThrownBy(() -> new RequestSizeLimitFilter(invalidLimit));
  }

  private static MockHttpServletRequest postWithBodyOfSize(int size) {
    var request =
        new MockHttpServletRequest("POST", "/api/v1/allocation-simulations/contributions");
    request.setContentType("application/json");
    request.setContent(new byte[size]);
    return request;
  }

  private static MockHttpServletRequest unknownLengthRequest(byte[] body) {
    var request = new UnknownLengthRequest();
    request.setContent(body);
    return request;
  }

  private static class UnknownLengthRequest extends MockHttpServletRequest {

    UnknownLengthRequest() {
      super("POST", "/api/v1/allocation-simulations/contributions");
      setContentType("application/json");
    }

    @Override
    public int getContentLength() {
      return -1;
    }

    @Override
    public long getContentLengthLong() {
      return -1;
    }
  }
}
