package io.github.thiagojosetj.portfolio.allocation.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ContributionSimulationRequest(
    @NotBlank @Pattern(regexp = "BRL") String currency,
    @NotBlank String contribution,
    @NotEmpty @Size(max = 20) List<@NotNull @Valid AllocationInput> allocations) {

  public record AllocationInput(
      @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{0,39}") String classId,
      @NotBlank @Size(max = 60) String name,
      @NotBlank String currentAmount,
      @NotBlank String targetPercentage) {}
}
