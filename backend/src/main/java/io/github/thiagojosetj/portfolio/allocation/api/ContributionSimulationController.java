package io.github.thiagojosetj.portfolio.allocation.api;

import io.github.thiagojosetj.portfolio.allocation.application.ContributionSimulationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/allocation-simulations")
public class ContributionSimulationController {

  private final ContributionSimulationService service;
  private final ContributionSimulationRequestMapper requestMapper;
  private final ContributionSimulationResponseMapper responseMapper;

  public ContributionSimulationController(
      ContributionSimulationService service,
      ContributionSimulationRequestMapper requestMapper,
      ContributionSimulationResponseMapper responseMapper) {
    this.service = service;
    this.requestMapper = requestMapper;
    this.responseMapper = responseMapper;
  }

  @PostMapping("/contributions")
  ContributionSimulationResponse simulate(
      @Valid @RequestBody ContributionSimulationRequest request) {
    var mappedRequest = requestMapper.map(request);
    var plan = service.simulate(mappedRequest.allocations(), mappedRequest.contributionInCents());
    return responseMapper.map(plan);
  }
}
