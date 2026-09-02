package io.github.thiagojosetj.portfolio.allocation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thiagojosetj.portfolio.allocation.application.ContributionSimulationService;
import io.github.thiagojosetj.portfolio.shared.api.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class ContributionSimulationControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    var validator = new LocalValidatorFactoryBean();
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
            .build();
  }

  @Test
  void shouldReturnDeterministicContributionPlan() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/allocation-simulations/contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.method").value("PROPORTIONAL_MONETARY_DEFICIT_V1"))
        .andExpect(jsonPath("$.currentTotal").value("10000.00"))
        .andExpect(jsonPath("$.contribution").value("500.00"))
        .andExpect(jsonPath("$.allocations[0].suggestedContribution").value("0.00"))
        .andExpect(jsonPath("$.allocations[1].suggestedContribution").value("375.00"))
        .andExpect(jsonPath("$.allocations[2].suggestedContribution").value("79.55"))
        .andExpect(jsonPath("$.allocations[3].suggestedContribution").value("45.45"));
  }

  @Test
  void shouldReturnProblemDetailsForInvalidTargetSum() throws Exception {
    var request = validRequest().replace("\"20.0000\"", "\"19.0000\"");

    mockMvc
        .perform(
            post("/api/v1/allocation-simulations/contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.type").value("urn:problem:invalid-simulation"))
        .andExpect(jsonPath("$.code").value("INVALID_TARGET_SUM"));
  }

  @Test
  void shouldRejectCommaDecimalAtTheApiBoundary() throws Exception {
    var request = validRequest().replace("\"500.00\"", "\"500,00\"");

    mockMvc
        .perform(
            post("/api/v1/allocation-simulations/contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.field").value("contribution"))
        .andExpect(jsonPath("$.code").value("INVALID_MONEY"));
  }

  @Test
  void shouldRejectNullAllocationItemAsInvalidRequest() throws Exception {
    var request =
        """
        {
          "currency": "BRL",
          "contribution": "100.00",
          "allocations": [null]
        }
        """;

    mockMvc
        .perform(
            post("/api/v1/allocation-simulations/contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("urn:problem:invalid-request"))
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void shouldReturnUniformProblemDetailsForMalformedJson() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/allocation-simulations/contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("urn:problem:invalid-json"))
        .andExpect(jsonPath("$.code").value("INVALID_JSON"));
  }

  @Test
  void shouldRejectPercentageAboveOneHundredAtTheApiBoundary() throws Exception {
    var request = validRequest().replace("\"40.0000\"", "\"101.0000\"");

    mockMvc
        .perform(
            post("/api/v1/allocation-simulations/contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("INVALID_PERCENTAGE"));
  }

  private String validRequest() {
    return """
                {
                  "currency": "BRL",
                  "contribution": "500.00",
                  "allocations": [
                    {"classId":"stocks","name":"Ações","currentAmount":"4800.00","targetPercentage":"40.0000"},
                    {"classId":"real-estate-funds","name":"FIIs","currentAmount":"1800.00","targetPercentage":"25.0000"},
                    {"classId":"etfs","name":"ETFs","currentAmount":"1400.00","targetPercentage":"15.0000"},
                    {"classId":"fixed-income","name":"Renda fixa","currentAmount":"2000.00","targetPercentage":"20.0000"}
                  ]
                }
                """;
  }
}
