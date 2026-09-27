package io.github.thiagojosetj.portfolio.management.persistence;

import java.time.Instant;
import java.util.List;
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
      select new io.github.thiagojosetj.portfolio.management.persistence.OwnedPortfolioSnapshotRow(
        p.id, p.ownerUserId, p.name, p.baseCurrency, p.version, p.createdAt, p.updatedAt,
        a.id, a.name, a.displayOrder, a.targetPercentage, a.createdAt, a.updatedAt
      )
      from PortfolioJpaEntity p
      left join AllocationClassJpaEntity a on a.portfolioId = p.id
      where p.id = :portfolioId
        and p.ownerUserId = :ownerUserId
      order by a.displayOrder
      """)
  List<OwnedPortfolioSnapshotRow> findOwnedSnapshotRows(
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
