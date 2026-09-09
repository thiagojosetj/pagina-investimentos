package io.github.thiagojosetj.portfolio.management.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolio")
public class PortfolioJpaEntity {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "owner_user_id", nullable = false, updatable = false)
  private UUID ownerUserId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "base_currency", nullable = false, length = 3)
  private String baseCurrency;

  @Version
  @Column(nullable = false)
  private Long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected PortfolioJpaEntity() {}

  private PortfolioJpaEntity(UUID id, UUID ownerUserId, String name, Instant now) {
    this.id = id;
    this.ownerUserId = ownerUserId;
    this.name = name;
    this.baseCurrency = "BRL";
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static PortfolioJpaEntity create(UUID id, UUID ownerUserId, String name, Instant now) {
    return new PortfolioJpaEntity(id, ownerUserId, name, now);
  }

  public UUID getId() {
    return id;
  }

  public UUID getOwnerUserId() {
    return ownerUserId;
  }

  public String getName() {
    return name;
  }

  public String getBaseCurrency() {
    return baseCurrency;
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
