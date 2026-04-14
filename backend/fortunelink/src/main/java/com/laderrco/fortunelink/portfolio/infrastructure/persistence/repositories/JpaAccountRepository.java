package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.services.projectors.AssetBalanceProjection;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSummaryProjection;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSymbolProjection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAccountRepository extends JpaRepository<AccountJpaEntity, UUID> {
  /**
   * Paginated summary projection for a portfolio's accounts.
   * <p>
   * Note: uses {@code a.portfolio.id} , AccountJpaEntity has a @ManyToOne relationship, not a
   * direct portfolioId column.
   * <p>
   * Explicitly excludes CLOSED accounts from the default list view. Callers that need closed
   * accounts should add a separate query.
   */
  @Query("""
      SELECT
          a.id            AS id,
          a.name          AS name,
          a.accountType   AS accountType,
          a.baseCurrencyCode AS baseCurrencyCode,
          a.positionStrategy AS positionStrategy,
          a.healthStatus  AS healthStatus,
          a.lifecycleState AS lifecycleState,
          a.cashBalanceAmount AS cashBalanceAmount,
          a.cashBalanceCurrency AS cashBalanceCurrency,
          a.createdDate   AS createdDate,
          a.lastUpdatedOn AS lastUpdatedOn
      FROM AccountJpaEntity a
      WHERE a.portfolio.id = :portfolioId
        AND a.lifecycleState != 'CLOSED'
      """)
  Page<AccountSummaryProjection> findByPortfolioId(@Param("portfolioId") UUID portfolioId,
      Pageable pageable);

  /**
   * Returns one row per (accountId, symbol) for every open position across the supplied account
   * IDs.
   * <p>
   * Sourced from PositionJpaEntity , current open holdings only. Symbols that have been fully sold
   * are no longer in positions, so they will not appear here. This is the correct behaviour for
   * quote fetching.
   */
  @Query("""
      SELECT
          p.account.id AS accountId,
          p.symbol     AS symbol
      FROM PositionJpaEntity p
      WHERE p.account.id IN :accountIds
      """)
  List<AccountSymbolProjection> findSymbolsForAccounts(@Param("accountIds") List<UUID> accountIds);

  @Query("""
      SELECT
          p.account.id AS accountId,
          p.symbol     AS symbol,
          p.quantity   AS quantity
      FROM PositionJpaEntity p
      WHERE p.account.id IN :accountIds
      """)
  List<AssetBalanceProjection> findBalancesForAccounts(@Param("accountIds") List<UUID> accountIds);

  @EntityGraph(attributePaths = {"positions", "realizedGains"})
  @Query("""
      SELECT a FROM AccountJpaEntity a
      WHERE a.id = :accountId
        AND a.portfolio.id = :portfolioId
        AND a.portfolio.userId = :userId
      """)
  Optional<AccountJpaEntity> findByIdWithOwnershipCheck(@Param("accountId") UUID accountId,
      @Param("portfolioId") UUID portfolioId, @Param("userId") UUID userId);

}
