package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.GainsAggregation;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.RealizedGainJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaRealizedGainRepository extends JpaRepository<RealizedGainJpaEntity, UUID> {

  @Query("""
      SELECT r FROM RealizedGainJpaEntity r
      WHERE r.account.id = :accountId
      ORDER BY r.occurredAt DESC
      """)
  Page<RealizedGainJpaEntity> findByAccountId(@Param("accountId") UUID accountId,
      Pageable pageable);

  @Query("""
      SELECT r FROM RealizedGainJpaEntity r
      WHERE r.account.id = :accountId
        AND EXTRACT(YEAR FROM r.occurredAt) = :year
      ORDER BY r.occurredAt DESC
      """)
  Page<RealizedGainJpaEntity> findByAccountIdAndYear(@Param("accountId") UUID accountId,
      @Param("year") int year, Pageable pageable);

  @Query("""
      SELECT r FROM RealizedGainJpaEntity r
      WHERE r.account.id = :accountId
        AND r.symbol = :symbol
      ORDER BY r.occurredAt DESC
      """)
  Page<RealizedGainJpaEntity> findByAccountIdAndSymbol(@Param("accountId") UUID accountId,
      @Param("symbol") String symbol, Pageable pageable);

  @Query("""
      SELECT r FROM RealizedGainJpaEntity r
      WHERE r.account.id = :accountId
        AND r.symbol = :symbol
        AND EXTRACT(YEAR FROM r.occurredAt) = :year
      ORDER BY r.occurredAt DESC
      """)
  Page<RealizedGainJpaEntity> findByAccountIdAndYearAndSymbol(@Param("accountId") UUID accountId,
      @Param("year") int year, @Param("symbol") String symbol, Pageable pageable);

  // Pull just the currency without loading the full account graph.
  @Query("SELECT a.baseCurrencyCode FROM AccountJpaEntity a WHERE a.id = :accountId")
  Optional<String> findAccountCurrencyById(@Param("accountId") UUID accountId);

  @Query("""
      SELECT new com.laderrco.fortunelink.portfolio.application.utils.valueobjects.GainsAggregation(
          SUM(CASE WHEN r.gainLossAmount > 0 THEN r.gainLossAmount ELSE 0 END),
          SUM(CASE WHEN r.gainLossAmount < 0 THEN ABS(r.gainLossAmount) ELSE 0 END)
      )
      FROM RealizedGainJpaEntity r
      WHERE r.account.id = :accountId
        AND (:symbol IS NULL OR r.symbol = :symbol)
        AND (:taxYear IS NULL OR EXTRACT(YEAR FROM r.occurredAt) = :taxYear)
      """)
  GainsAggregation calculateTotals(@Param("accountId") UUID accountId,
      @Param("taxYear") Integer taxYear, @Param("symbol") String symbol);
}