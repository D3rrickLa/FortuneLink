package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.TransactionJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.valueobjects.FeeAggregationResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {

  // --- Find Methods (List) ---
  List<TransactionJpaEntity> findByPortfolioIdAndAccountId(UUID portfolioId, UUID accountId);

  List<TransactionJpaEntity> findByAccountIdAndExecutionSymbol(UUID accountId, String symbol);

  List<TransactionJpaEntity> findByAccountIdAndOccurredAtBetween(UUID accountId, Instant start,
      Instant end);

  Optional<TransactionJpaEntity> findByIdAndPortfolioIdAndAccountId(UUID id, UUID portfolioId,
      UUID accountId);

  // --- Find Methods (Paginated) ---
  Page<TransactionJpaEntity> findByAccountId(UUID accountId, Pageable pageable);

  Page<TransactionJpaEntity> findByAccountIdAndOccurredAtBetween(UUID accountId, Instant start,
      Instant end, Pageable pageable);

  Page<TransactionJpaEntity> findByAccountIdAndExecutionSymbol(UUID accountId, String symbol,
      Pageable pageable);

  // --- Custom Queries ---
  @Query("""
      SELECT t FROM TransactionJpaEntity t
      JOIN PortfolioJpaEntity p ON p.id = t.portfolioId
      WHERE t.portfolioId = :portfolioId
        AND p.userId = :userId
        AND t.accountId = :accountId
      """)
  List<TransactionJpaEntity> findByPortfolioIdAndUserIdAndAccountId(
      @Param("portfolioId") UUID portfolioId, @Param("userId") UUID userId,
      @Param("accountId") UUID accountId);

  /**
   * Used in the 'save' logic to find the denormalized portfolioId when it's not
   * provided. Assumes a
   * relationship exists between Account and Portfolio.
   */
  @Query("SELECT a.portfolio.id FROM AccountJpaEntity a WHERE a.id = :accountId")
  UUID findPortfolioIdByAccountId(@Param("accountId") UUID accountId);

  /**
   * Uses accountAmount when set (post-conversion), falls back to nativeAmount.
   * This is safe only
   * because Fee.withConversion() guarantees accountAmount is always in account
   * base currency.
   */
  @Query("""
      SELECT t.accountId as accountId,
             t.executionSymbol as symbol,
             SUM(COALESCE(f.accountAmount, f.nativeAmount)) as totalFees,
             t.cashDeltaCurrency as currency
      FROM TransactionJpaEntity t
      JOIN t.fees f
      WHERE t.accountId IN :accountIds
        AND t.transactionType = 'BUY'
        AND t.excluded = false
        AND t.executionSymbol IS NOT NULL
      GROUP BY t.accountId, t.executionSymbol, t.cashDeltaCurrency
      """)
  List<FeeAggregationResult> sumBuyFeesByAccountAndSymbol(@Param("accountIds") List<UUID> accountIds);

  @Query("""
      SELECT t FROM TransactionJpaEntity t
      WHERE t.accountId = :accountId
        AND (CAST(:symbol AS string) IS NULL OR t.executionSymbol = :symbol)
        AND (CAST(:startDate AS instant) IS NULL OR t.occurredAt >= :startDate)
        AND (CAST(:endDate AS instant) IS NULL OR t.occurredAt <= :endDate)
      """)
  Page<TransactionJpaEntity> findTransactionsDynamic(
      @Param("accountId") UUID accountId,
      @Param("symbol") String symbol,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate,
      Pageable pageable);

  // --- Deletion Logic ---
  @Modifying
  @Query("DELETE FROM TransactionJpaEntity t WHERE t.accountId = :accountId AND t.excluded = true AND t.excludedAt < :cutoff")
  int deleteExpiredTransactions(@Param("accountId") UUID accountId,
      @Param("cutoff") Instant cutoff);

  @Modifying
  @Query("DELETE FROM TransactionJpaEntity t WHERE t.excluded = true AND t.excludedAt < :cutoff")
  int deleteAllExpiredTransactions(@Param("cutoff") Instant cutoff);

  @Query("""
      SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END
      FROM TransactionJpaEntity t
      WHERE t.accountId = :accountId
        AND t.transactionType = :type
        AND t.executionSymbol = :symbol
        AND t.occurredAt BETWEEN :start AND :end
      """)
  boolean existsConflict(@Param("accountId") UUID accountId, @Param("type") TransactionType type,
      @Param("symbol") String symbol, @Param("start") Instant start, @Param("end") Instant end);

  Optional<TransactionJpaEntity> findByIdempotencyKey(String key);

  Optional<TransactionJpaEntity> findByIdempotencyKeyAndPortfolioId(String key, @Param("portfolioId") UUID portfolioId);

  @Query("""
      SELECT COUNT(t) FROM TransactionJpaEntity t
      WHERE t.accountId = :accountId
        AND t.excluded = true
        AND t.transactionType IN (
            'BUY', 'SELL', 'DIVIDEND', 'DIVIDEND_REINVEST',
            'INTEREST', 'RETURN_OF_CAPITAL', 'SPLIT'
        )
      """)
  long countExcludedPositionAffecting(@Param("accountId") UUID accountId);
}
