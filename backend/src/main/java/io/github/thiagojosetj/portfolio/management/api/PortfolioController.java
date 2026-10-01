package io.github.thiagojosetj.portfolio.management.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.github.thiagojosetj.portfolio.identity.security.SessionPrincipal;
import io.github.thiagojosetj.portfolio.management.application.CreatePortfolioCommand;
import io.github.thiagojosetj.portfolio.management.application.PortfolioManagementService;
import io.github.thiagojosetj.portfolio.management.application.PortfolioView;
import io.github.thiagojosetj.portfolio.management.application.ReplaceAllocationTargetsCommand;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetDefinition;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetUpdate;
import io.github.thiagojosetj.portfolio.management.domain.PortfolioValidationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolios")
@Profile("wallet | local-wallet")
public class PortfolioController {
  private final PortfolioManagementService service;

  public PortfolioController(PortfolioManagementService service) {
    this.service = service;
  }

  @GetMapping
  public PortfolioList list(@AuthenticationPrincipal SessionPrincipal user) {
    return new PortfolioList(
        service.listPortfolios(user.userId()).stream().map(PortfolioResponse::from).toList());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PortfolioResponse create(
      @AuthenticationPrincipal SessionPrincipal user, @Valid @RequestBody CreateRequest request) {
    return PortfolioResponse.from(
        service.createPortfolio(
            new CreatePortfolioCommand(
                user.userId(),
                request.name(),
                List.of(
                    target("Ações", "40"),
                    target("FIIs", "25"),
                    target("ETFs", "15"),
                    target("Renda fixa", "20")))));
  }

  @GetMapping("/{id}")
  public PortfolioResponse find(
      @AuthenticationPrincipal SessionPrincipal user, @PathVariable UUID id) {
    return PortfolioResponse.from(service.findPortfolio(user.userId(), id));
  }

  @PutMapping("/{id}/targets")
  public PortfolioResponse targets(
      @AuthenticationPrincipal SessionPrincipal user,
      @PathVariable UUID id,
      @Valid @RequestBody ReplaceTargetsRequest request) {
    for (int index = 0; index < request.targets().size(); index++) {
      if (request.targets().get(index).displayOrder() != index) {
        throw new PortfolioValidationException(
            "targets.displayOrder", "order", "A ordem deve corresponder à posição de cada classe.");
      }
    }
    var updates =
        request.targets().stream()
            .map(
                item ->
                    new AllocationTargetUpdate(
                        item.id(), item.name(), new BigDecimal(item.targetPercentage())))
            .toList();
    return PortfolioResponse.from(
        service.replaceAllocationTargets(
            new ReplaceAllocationTargetsCommand(
                user.userId(), id, request.expectedVersion(), updates)));
  }

  private static AllocationTargetDefinition target(String name, String percentage) {
    return new AllocationTargetDefinition(name, new BigDecimal(percentage));
  }

  public record CreateRequest(
      @NotBlank @Size(max = 100) String name, @NotNull @Pattern(regexp = "BRL") String currency) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
      throw new IllegalArgumentException("Campo não reconhecido.");
    }
  }

  public record ReplaceTargetsRequest(
      @NotNull Long expectedVersion,
      @NotNull @Size(min = 1, max = 20) List<@NotNull @Valid TargetRequest> targets) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
      throw new IllegalArgumentException("Campo não reconhecido.");
    }
  }

  public record TargetRequest(
      UUID id,
      @NotBlank @Size(max = 60) String name,
      @NotNull @Pattern(regexp = "(?:0|[1-9][0-9]{0,2})(?:\\.[0-9]{1,4})?") String targetPercentage,
      @NotNull Integer displayOrder) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
      throw new IllegalArgumentException("Campo não reconhecido.");
    }
  }

  public record PortfolioList(List<PortfolioResponse> portfolios) {}

  public record PortfolioResponse(
      UUID id,
      String name,
      String currency,
      long version,
      Instant createdAt,
      Instant updatedAt,
      List<TargetResponse> targets) {
    static PortfolioResponse from(PortfolioView portfolio) {
      return new PortfolioResponse(
          portfolio.id(),
          portfolio.name(),
          portfolio.baseCurrency(),
          portfolio.version(),
          portfolio.createdAt(),
          portfolio.updatedAt(),
          portfolio.allocationTargets().stream()
              .map(
                  item ->
                      new TargetResponse(
                          item.id(),
                          item.name(),
                          item.targetPercentage().toPlainString(),
                          item.displayOrder()))
              .toList());
    }
  }

  public record TargetResponse(UUID id, String name, String targetPercentage, int displayOrder) {}
}
