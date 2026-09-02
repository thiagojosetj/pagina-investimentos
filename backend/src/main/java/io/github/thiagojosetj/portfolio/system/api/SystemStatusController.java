package io.github.thiagojosetj.portfolio.system.api;

import java.time.Clock;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

  private final Clock clock;

  public SystemStatusController(Clock clock) {
    this.clock = clock;
  }

  @GetMapping("/status")
  SystemStatusResponse status() {
    return new SystemStatusResponse("UP", "portfolio-api", "0.1.0", Instant.now(clock));
  }

  record SystemStatusResponse(String status, String service, String version, Instant timestamp) {}
}
