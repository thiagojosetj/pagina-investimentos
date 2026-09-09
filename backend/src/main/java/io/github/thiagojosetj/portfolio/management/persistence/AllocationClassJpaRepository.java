package io.github.thiagojosetj.portfolio.management.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AllocationClassJpaRepository extends Repository<AllocationClassJpaEntity, UUID> {

  List<AllocationClassJpaEntity> saveAll(Iterable<AllocationClassJpaEntity> targets);

  void flush();

  @Query(
      """
      select a
      from AllocationClassJpaEntity a
      where a.portfolioId = :portfolioId
        and exists (
          select p.id
          from PortfolioJpaEntity p
          where p.id = a.portfolioId
            and p.ownerUserId = :ownerUserId
        )
      order by a.displayOrder
      """)
  List<AllocationClassJpaEntity> findAllOwnedByPortfolioId(
      @Param("ownerUserId") UUID ownerUserId, @Param("portfolioId") UUID portfolioId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          """
          DELETE FROM allocation_class AS allocation
          USING portfolio
          WHERE allocation.portfolio_id = portfolio.id
            AND portfolio.id = :portfolioId
            AND portfolio.owner_user_id = :ownerUserId
          """,
      nativeQuery = true)
  int deleteAllOwnedByPortfolioId(
      @Param("ownerUserId") UUID ownerUserId, @Param("portfolioId") UUID portfolioId);
}
