package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.ValuationSnapshotJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaValuationSnapshotRepository extends
    JpaRepository<ValuationSnapshotJpaEntity, UUID> {

  /**
   * Snapshots in ascending date order for chart rendering (oldest → newest).
   * Caller determines the
   * window , typically 90 or 365 days.
   */
  @Query("""
      SELECT s FROM ValuationSnapshotJpaEntity s
      WHERE s.userId = :userId
        AND s.snapshotDate >= :since
      ORDER BY s.snapshotDate ASC
      """)
  List<ValuationSnapshotJpaEntity> findByUserIdSince(@Param("userId") UUID userId,
      @Param("since") Instant since);

  /**
   * Day-boundary check. NEVER use DATE() in JPQL against a TIMESTAMPTZ column ,
   * the cast behavior
   * is JVM-timezone-dependent. Instead, pass explicit UTC bounds computed by the
   * caller (start of
   * today UTC, start of tomorrow UTC).
   *
   * <p>
   * The caller in {@code NetWorthSnapshotRepositoryImpl.existsForToday()}
   * computes these bounds and
   * passes them here.
   */
  @Query("""
      SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
      FROM ValuationSnapshotJpaEntity s
      WHERE s.userId = :userId
        AND s.snapshotDate >= :startOfDay
        AND s.snapshotDate < :endOfDay
      """)
  boolean existsBetween(@Param("userId") UUID userId, @Param("startOfDay") Instant startOfDay,
      @Param("endOfDay") Instant endOfDay);

  @Query("""
      SELECT s FROM ValuationSnapshotJpaEntity s
      WHERE s.userId = :userId
        AND s.snapshotDate >= :startOfDay
        AND s.snapshotDate < :endOfDay
      """)
  Optional<ValuationSnapshotJpaEntity> findByUserIdAndSnapshotDate(
      @Param("userId") UUID userId,
      @Param("startOfDay") Instant startOfDay,
      @Param("endOfDay") Instant endOfDay);

  @Modifying
  @Query(value = """
      INSERT INTO portfolio_valuation_snapshots (
          id,
          user_id,
          total_value_amount,
          total_value_currency,
          total_cost_basis_amount,
          total_cost_basis_currency,
          unrealized_gain_loss_amount,
          unrealized_gain_loss_currency,
          gain_loss_percent,
          total_cash_balance_amount,
          total_cash_balance_currency,
          total_invested_value_amount,
          total_invested_value_currency,
          display_currency_code,
          has_stale_data,
          snapshot_date,
          snapshot_day,
          created_at
      ) VALUES (
          :id,
          :userId,
          :totalValueAmount,
          :totalValueCurrency,
          :totalCostBasisAmount,
          :totalCostBasisCurrency,
          :unrealizedGainLossAmount,
          :unrealizedGainLossCurrency,
          :gainLossPercent,
          :totalCashBalanceAmount,
          :totalCashBalanceCurrency,
          :totalInvestedValueAmount,
          :totalInvestedValueCurrency,
          :displayCurrencyCode,
          :hasStaleData,
          :snapshotDate,
          :snapshotDay,
          :createdAt
      )
      ON CONFLICT (user_id, snapshot_day)
      DO UPDATE SET
          total_value_amount = EXCLUDED.total_value_amount,
          total_value_currency = EXCLUDED.total_value_currency,
          total_cost_basis_amount = EXCLUDED.total_cost_basis_amount,
          total_cost_basis_currency = EXCLUDED.total_cost_basis_currency,
          unrealized_gain_loss_amount = EXCLUDED.unrealized_gain_loss_amount,
          unrealized_gain_loss_currency = EXCLUDED.unrealized_gain_loss_currency,
          gain_loss_percent = EXCLUDED.gain_loss_percent,
          total_cash_balance_amount = EXCLUDED.total_cash_balance_amount,
          total_cash_balance_currency = EXCLUDED.total_cash_balance_currency,
          total_invested_value_amount = EXCLUDED.total_invested_value_amount,
          total_invested_value_currency = EXCLUDED.total_invested_value_currency,
          display_currency_code = EXCLUDED.display_currency_code,
          has_stale_data = EXCLUDED.has_stale_data,
          snapshot_date = EXCLUDED.snapshot_date
      """, nativeQuery = true)
  void upsertSnapshot(
      UUID id,
      UUID userId,
      BigDecimal totalValueAmount,
      String totalValueCurrency,
      BigDecimal totalCostBasisAmount,
      String totalCostBasisCurrency,
      BigDecimal unrealizedGainLossAmount,
      String unrealizedGainLossCurrency,
      BigDecimal gainLossPercent,
      BigDecimal totalCashBalanceAmount,
      String totalCashBalanceCurrency,
      BigDecimal totalInvestedValueAmount,
      String totalInvestedValueCurrency,
      String displayCurrencyCode,
      boolean hasStaleData,
      Instant snapshotDate,
      LocalDate snapshotDay,
      Instant createdAt);
}
