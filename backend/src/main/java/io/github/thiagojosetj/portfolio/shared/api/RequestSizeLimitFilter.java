package io.github.thiagojosetj.portfolio.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Recusa corpos declarados acima do limite e limita os bytes reais dos comandos JSON síncronos
 * antes da desserialização, mesmo sem {@code Content-Length} (por exemplo, chunked).
 *
 * <p>A propriedade {@code server.tomcat.max-http-post-size} não cobre este caso: ela limita apenas
 * corpos de formulário que o Tomcat converte em parâmetros. Um corpo {@code application/json} é
 * lido direto pelo conversor de mensagem do Spring, então sem este filtro a lista de alocações é
 * materializada inteira em memória antes de a validação {@code @Size(max = 20)} rejeitá-la.
 *
 * <p>A ordem é a mais alta possível para que o filtro rode antes de qualquer outro da cadeia,
 * inclusive do {@code OrderedFormContentFilter}, que consome o corpo de requisições {@code
 * form-urlencoded} em PUT, PATCH e DELETE.
 *
 * <p>A pré-leitura consome no máximo o limite mais um byte. Formulários, multipart e métodos de
 * leitura não são consumidos: sua interpretação pertence ao container. Novos endpoints de upload,
 * streaming ou leitura assíncrona exigem política própria, assim como timeouts e rate limiting em
 * um futuro deploy público.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

  private static final Set<String> JSON_BODY_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

  private static final String PROBLEM_BODY =
      """
      {"type":"urn:problem:payload-too-large",\
      "title":"Requisição muito grande",\
      "status":413,\
      "detail":"O corpo da requisição excede o limite aceito pela API."}""";

  private final int maxRequestBytes;

  RequestSizeLimitFilter(@Value("${portfolio.api.max-request-bytes:65536}") long maxRequestBytes) {
    if (maxRequestBytes <= 0 || maxRequestBytes >= Integer.MAX_VALUE) {
      throw new IllegalArgumentException(
          "portfolio.api.max-request-bytes must be between 1 and 2147483646");
    }
    this.maxRequestBytes = (int) maxRequestBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxRequestBytes) {
      rejectOversizedRequest(response);
      return;
    }
    if (!hasSynchronousJsonBody(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    byte[] body = request.getInputStream().readNBytes(maxRequestBytes + 1);
    if (body.length > maxRequestBytes) {
      rejectOversizedRequest(response);
      return;
    }
    filterChain.doFilter(new BufferedJsonBodyRequest(request, body), response);
  }

  private static boolean hasSynchronousJsonBody(HttpServletRequest request) {
    if (!JSON_BODY_METHODS.contains(request.getMethod()) || request.getContentType() == null) {
      return false;
    }
    try {
      MediaType contentType = MediaType.parseMediaType(request.getContentType());
      return MediaType.APPLICATION_JSON.includes(contentType)
          || ("application".equals(contentType.getType())
              && contentType.getSubtype().endsWith("+json"));
    } catch (InvalidMediaTypeException exception) {
      // A fronteira MVC mantém a responsabilidade pela resposta a um Content-Type inválido.
      return false;
    }
  }

  private static void rejectOversizedRequest(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(PROBLEM_BODY);
  }
}
