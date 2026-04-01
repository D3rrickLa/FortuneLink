package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

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

// we would need to change this to the Entity version
@Repository
public interface JpaTransactionRepository extends JpaRepository<TransactionJpaEntity, UUID> {

  // --- Find Methods (List) ---
  List<TransactionJpaEntity> findByPortfolioIdAndAccountId(UUID portfolioId, UUID accountId);

  List<TransactionJpaEntity> findByAccountIdAndSymbol(UUID accountId, String symbol);

  List<TransactionJpaEntity> findByAccountIdAndOccurredAtBetween(UUID accountId, Instant start, Instant end);

  Optional<TransactionJpaEntity> findByIdAndPortfolioIdAndAccountId(UUID id, UUID portfolioId, UUID accountId);

  // --- Find Methods (Paginated) ---
  Page<TransactionJpaEntity> findByAccountId(UUID accountId, Pageable pageable);

  Page<TransactionJpaEntity> findByAccountIdAndOccurredAtBetween(UUID accountId, Instant start, Instant end,
      Pageable pageable);

  Page<TransactionJpaEntity> findByAccountIdAndSymbol(UUID accountId, String symbol, Pageable pageable);

  // --- Custom Queries ---
  /**
   * Used in the 'save' logic to find the denormalized portfolioId when it's not
   * provided.
   * Assumes a relationship exists between Account and Portfolio.
   */
  @Query("SELECT a.portfolio.id FROM AccountEntity a WHERE a.id = :accountId")
  UUID findPortfolioIdByAccountId(@Param("accountId") UUID accountId);

  @Query("""
      SELECT t.account.id as accountId,
             t.execution.asset as symbol,
             SUM(f.accountAmount.amount) as totalFees,
             t.account.base_currency as currency
      FROM Transaction t
      JOIN t.fees f
      WHERE t.account.id IN :accountIds
        AND t.transactionType = 'BUY'
        AND t.metadata.exclusion IS NULL
      GROUP BY t.account.id, t.execution.asset
      """)
  List<FeeAggregationResult> sumBuyFeesByAccountAndSymbol(@Param("accountIds") List<UUID> accountIds);

  // --- Deletion Logic ---
  @Modifying
  @Query("DELETE FROM Transaction t " + "WHERE t.account.id = :accountId " + "AND t.excluded = true "
      + "AND t.metadata.excludedAt < :cutoff")
  int deleteExpiredTransactions(@Param("accountId") UUID accountId, @Param("cutoff") Instant cutoff);

  @Modifying
  @Query("DELETE FROM TransactionJpaEntity t WHERE t.occurredAt < :cutoff")
  int deleteAllExpiredTransactions(@Param("cutoff") Instant cutoff);

}
