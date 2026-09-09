package io.github.thiagojosetj.portfolio.management.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "allocation_class")
public class AllocationClassJpaEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "portfolio_id", nullable = false, updatable = false)
  private UUID portfolioId;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(name = "display_order", nullable = false)
  private Short displayOrder;

  @Column(name = "target_percentage", nullable = false, precision = 7, scale = 4)
  private BigDecimal targetPercentage;

  @Version
  @Column(nullable = false)
  private Long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AllocationClassJpaEntity() {}

  private AllocationClassJpaEntity(
      UUID id,
      UUID portfolioId,
      String name,
      short displayOrder,
      BigDecimal targetPercentage,
      Instant now) {
    this.id = id;
    this.portfolioId = portfolioId;
    this.name = name;
    this.displayOrder = displayOrder;
    this.targetPercentage = targetPercentage;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static AllocationClassJpaEntity create(
      UUID id,
      UUID portfolioId,
      String name,
      short displayOrder,
      BigDecimal targetPercentage,
      Instant now) {
    return new AllocationClassJpaEntity(id, portfolioId, name, displayOrder, targetPercentage, now);
  }

  public UUID getId() {
    return id;
  }

  public UUID getPortfolioId() {
    return portfolioId;
  }

  public String getName() {
    return name;
  }

  public Short getDisplayOrder() {
    return displayOrder;
  }

  public BigDecimal getTargetPercentage() {
    return targetPercentage;
  }

  public Long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
