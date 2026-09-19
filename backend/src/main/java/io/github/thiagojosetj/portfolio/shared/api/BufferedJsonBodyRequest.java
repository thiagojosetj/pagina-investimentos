package io.github.thiagojosetj.portfolio.shared.api;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/** Repassa o corpo JSON limitado ao MVC síncrono, sem reler o stream de rede já consumido. */
final class BufferedJsonBodyRequest extends HttpServletRequestWrapper {

  private static final String ASYNC_NOT_SUPPORTED =
      "Buffered JSON request bodies support synchronous reads only";

  private final ServletInputStream inputStream;
  private boolean inputStreamRequested;
  private BufferedReader reader;

  BufferedJsonBodyRequest(HttpServletRequest request, byte[] body) {
    super(request);
    var buffer = new ByteArrayInputStream(body);
    inputStream =
        new ServletInputStream() {
          @Override
          public int read() {
            return buffer.read();
          }

          @Override
          public int read(byte[] bytes, int offset, int length) {
            return buffer.read(bytes, offset, length);
          }

          @Override
          public int available() {
            return buffer.available();
          }

          @Override
          public boolean isFinished() {
            return buffer.available() == 0;
          }

          @Override
          public boolean isReady() {
            return true;
          }

          @Override
          public void setReadListener(ReadListener listener) {
            throw new IllegalStateException(ASYNC_NOT_SUPPORTED);
          }
        };
  }

  @Override
  public ServletInputStream getInputStream() {
    if (reader != null) {
      throw new IllegalStateException("getReader() has already been called for this request");
    }
    inputStreamRequested = true;
    return inputStream;
  }

  @Override
  public BufferedReader getReader() throws UnsupportedEncodingException {
    if (inputStreamRequested) {
      throw new IllegalStateException("getInputStream() has already been called for this request");
    }
    if (reader == null) {
      String encoding = getCharacterEncoding();
      reader =
          new BufferedReader(
              new InputStreamReader(
                  inputStream, encoding == null ? StandardCharsets.ISO_8859_1.name() : encoding));
    }
    return reader;
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

  @Override
  public AsyncContext startAsync() {
    throw new IllegalStateException(ASYNC_NOT_SUPPORTED);
  }

  @Override
  public AsyncContext startAsync(ServletRequest request, ServletResponse response) {
    throw new IllegalStateException(ASYNC_NOT_SUPPORTED);
  }
}
