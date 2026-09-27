package io.github.thiagojosetj.portfolio.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("simulator")
class LocalSwaggerSecurityPolicyTest {

  @Autowired MockMvc mockMvc;

  @Test
  void localSwaggerRemainsAvailableWithoutPublicDemoCsp() throws Exception {
    mockMvc
        .perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Content-Security-Policy"));
  }
}
