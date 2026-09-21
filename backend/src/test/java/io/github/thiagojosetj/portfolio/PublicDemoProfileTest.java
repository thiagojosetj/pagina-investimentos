package io.github.thiagojosetj.portfolio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.thiagojosetj.portfolio.management.application.PortfolioManagementService;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PublicDemoProfileTest {

  @Autowired MockMvc mockMvc;

  @Autowired ApplicationContext context;

  @Test
  void exposesOnlyHealthAndStatelessApiWithoutPersistence() throws Exception {
    assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
    assertThat(context.getBeanNamesForType(Flyway.class)).isEmpty();
    assertThat(context.getBeanNamesForType(EntityManagerFactory.class)).isEmpty();
    assertThat(context.getBeanNamesForType(PortfolioManagementService.class)).isEmpty();

    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/info")).andExpect(status().isNotFound());
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
  }

  @Test
  void calculatesContributionsWithoutDatabase() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/allocation-simulations/contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "currency": "BRL",
                      "contribution": "100.00",
                      "allocations": [
                        {"classId":"stocks","name":"Ações","currentAmount":"60.00","targetPercentage":"50.0000"},
                        {"classId":"funds","name":"Fundos","currentAmount":"40.00","targetPercentage":"50.0000"}
                      ]
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allocations[0].suggestedContribution").value("40.00"))
        .andExpect(jsonPath("$.allocations[1].suggestedContribution").value("60.00"));
  }

  @Test
  void rejectsInvalidTargetsWithoutDatabase() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/allocation-simulations/contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "currency": "BRL",
                      "contribution": "100.00",
                      "allocations": [
                        {"classId":"stocks","name":"Ações","currentAmount":"60.00","targetPercentage":"49.0000"},
                        {"classId":"funds","name":"Fundos","currentAmount":"40.00","targetPercentage":"50.0000"}
                      ]
                    }
                    """))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("INVALID_TARGET_SUM"));
  }

  @Test
  void keepsRequestSizeLimitInPublicMode() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/allocation-simulations/contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(" ".repeat(65_537)))
        .andExpect(status().isPayloadTooLarge());
  }
}
