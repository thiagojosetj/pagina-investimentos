package io.github.thiagojosetj.portfolio.shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Recusa requisições cujo corpo declarado excede o limite aceito, antes de qualquer
 * desserialização.
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
 * <p>Limitação conhecida: a checagem usa o {@code Content-Length} declarado. Uma requisição com
 * {@code Transfer-Encoding: chunked} não declara tamanho e passa por este filtro. O cliente desta
 * API envia corpo JSON com tamanho declarado, e a defesa principal contra tráfego externo é o
 * {@code server.address}, que prende a API ao loopback.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

  private static final String PROBLEM_BODY =
      """
      {"type":"urn:problem:payload-too-large",\
      "title":"Requisição muito grande",\
      "status":413,\
      "detail":"O corpo da requisição excede o limite aceito pela API."}""";

  private final long maxRequestBytes;

  RequestSizeLimitFilter(@Value("${portfolio.api.max-request-bytes:65536}") long maxRequestBytes) {
    this.maxRequestBytes = maxRequestBytes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > maxRequestBytes) {
      response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.getWriter().write(PROBLEM_BODY);
      return;
    }
    filterChain.doFilter(request, response);
  }
}
