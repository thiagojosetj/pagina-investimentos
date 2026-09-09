package io.github.thiagojosetj.portfolio.management.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PortfolioJpaRepository extends Repository<PortfolioJpaEntity, UUID> {

  PortfolioJpaEntity save(PortfolioJpaEntity portfolio);

  void flush();

  @Query(
      """
      select p
      from PortfolioJpaEntity p
      where p.id = :portfolioId
        and p.ownerUserId = :ownerUserId
      """)
  Optional<PortfolioJpaEntity> findOwnedById(
      @Param("ownerUserId") UUID ownerUserId, @Param("portfolioId") UUID portfolioId);

  boolean existsByIdAndOwnerUserId(UUID portfolioId, UUID ownerUserId);

  @Query(
      value = "SELECT EXISTS (SELECT 1 FROM app_user WHERE id = :ownerUserId)",
      nativeQuery = true)
  boolean ownerExists(@Param("ownerUserId") UUID ownerUserId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      update PortfolioJpaEntity p
         set p.version = p.version + 1,
             p.updatedAt = :updatedAt
       where p.id = :portfolioId
         and p.ownerUserId = :ownerUserId
         and p.version = :expectedVersion
      """)
  int claimOwnedVersion(
      @Param("ownerUserId") UUID ownerUserId,
      @Param("portfolioId") UUID portfolioId,
      @Param("expectedVersion") long expectedVersion,
      @Param("updatedAt") Instant updatedAt);
}
