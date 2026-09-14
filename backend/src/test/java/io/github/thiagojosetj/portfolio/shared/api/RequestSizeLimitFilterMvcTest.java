package io.github.thiagojosetj.portfolio.shared.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thiagojosetj.portfolio.allocation.api.ContributionSimulationController;
import io.github.thiagojosetj.portfolio.allocation.api.ContributionSimulationRequestMapper;
import io.github.thiagojosetj.portfolio.allocation.api.ContributionSimulationResponseMapper;
import io.github.thiagojosetj.portfolio.allocation.application.ContributionSimulationService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class RequestSizeLimitFilterMvcTest {

  private static final int LIMIT = 1024;

  private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    validator.afterPropertiesSet();
    var controller =
        new ContributionSimulationController(
            new ContributionSimulationService(),
            new ContributionSimulationRequestMapper(),
            new ContributionSimulationResponseMapper());
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ApiExceptionHandler())
            .setValidator(validator)
            .addFilters(new RequestSizeLimitFilter(LIMIT))
            .build();
  }

  @AfterEach
  void tearDown() {
    validator.close();
  }

  @Test
  void shouldKeepTheRealSimulationContractForUnknownLengthJsonAtTheLimit() throws Exception {
    String json =
        """
        {"currency":"BRL","contribution":"10.00","allocations":[
          {"classId":"stocks","name":"Ações","currentAmount":"100.00","targetPercentage":"100.0000"}
        ]}
        """;
    String atLimit = json + " ".repeat(LIMIT - json.getBytes(StandardCharsets.UTF_8).length);

    mockMvc
        .perform(unknownLengthJson(atLimit))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.method").value("PROPORTIONAL_MONETARY_DEFICIT_V1"))
        .andExpect(jsonPath("$.projectedTotal").value("110.00"))
        .andExpect(jsonPath("$.allocations[0].name").value("Ações"))
        .andExpect(jsonPath("$.allocations[0].suggestedContribution").value("10.00"));
  }

  @Test
  void shouldKeepTheMalformedJsonErrorBelowTheLimit() throws Exception {
    mockMvc
        .perform(unknownLengthJson("{not-json}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("urn:problem:invalid-json"))
        .andExpect(jsonPath("$.code").value("INVALID_JSON"));
  }

  @Test
  void shouldRejectOversizedUnknownLengthBodyBeforeJsonParsing() throws Exception {
    mockMvc
        .perform(unknownLengthJson("{" + " ".repeat(LIMIT)))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.type").value("urn:problem:payload-too-large"))
        .andExpect(jsonPath("$.title").value("Requisição muito grande"))
        .andExpect(jsonPath("$.status").value(413));
  }

  private static RequestBuilder unknownLengthJson(String body) {
    return servletContext -> {
      var request =
          new MockHttpServletRequest(
              servletContext, "POST", "/api/v1/allocation-simulations/contributions") {
            @Override
            public int getContentLength() {
              return -1;
            }

            @Override
            public long getContentLengthLong() {
              return -1;
            }
          };
      request.setContentType("application/json");
      request.addHeader("Transfer-Encoding", "chunked");
      request.setContent(body.getBytes(StandardCharsets.UTF_8));
      return request;
    };
  }
}
