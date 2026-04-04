package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.RealizedGainJpaEntity;

@Repository
public interface JpaRealizedGainRepository extends JpaRepository<RealizedGainJpaEntity, UUID> {

    @Query("""
        SELECT r FROM RealizedGainJpaEntity r
        WHERE r.account.id = :accountId
        ORDER BY r.occurredAt DESC
        """)
    List<RealizedGainJpaEntity> findByAccountId(@Param("accountId") UUID accountId);

    @Query("""
        SELECT r FROM RealizedGainJpaEntity r
        WHERE r.account.id = :accountId
          AND EXTRACT(YEAR FROM r.occurredAt) = :year
        ORDER BY r.occurredAt DESC
        """)
    List<RealizedGainJpaEntity> findByAccountIdAndYear(
        @Param("accountId") UUID accountId,
        @Param("year") int year);

    @Query("""
        SELECT r FROM RealizedGainJpaEntity r
        WHERE r.account.id = :accountId
          AND r.symbol = :symbol
        ORDER BY r.occurredAt DESC
        """)
    List<RealizedGainJpaEntity> findByAccountIdAndSymbol(
        @Param("accountId") UUID accountId,
        @Param("symbol") String symbol);

    @Query("""
        SELECT r FROM RealizedGainJpaEntity r
        WHERE r.account.id = :accountId
          AND r.symbol = :symbol
          AND EXTRACT(YEAR FROM r.occurredAt) = :year
        ORDER BY r.occurredAt DESC
        """)
    List<RealizedGainJpaEntity> findByAccountIdAndYearAndSymbol(
        @Param("accountId") UUID accountId,
        @Param("year") int year,
        @Param("symbol") String symbol);

    // Pull just the currency without loading the full account graph.
    @Query("SELECT a.baseCurrencyCode FROM AccountJpaEntity a WHERE a.id = :accountId")
    Optional<String> findAccountCurrencyById(@Param("accountId") UUID accountId);
}