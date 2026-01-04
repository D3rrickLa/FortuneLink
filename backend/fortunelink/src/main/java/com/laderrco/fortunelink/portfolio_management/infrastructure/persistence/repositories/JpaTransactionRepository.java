package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;

@Repository
public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    // Basic queries
    List<TransactionEntity> findByPortfolioId(UUID portfolioId, Pageable pageable);

    Page<TransactionEntity> findByAccountId(UUID accountId, Pageable pageable);

    long countByPortfolioId(UUID portfolioId);

    long countByAccountId(UUID accountId);

    // Filtered queries for portfolio
    @Query("SELECT t FROM TransactionEntity t WHERE t.portfolioId = :portfolioId " +
            "AND (:transactionType IS NULL OR t.transactionType = :transactionType) " +
            "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.transactionDate <= :endDate)")
    public Page<TransactionEntity> findByPortfolioIdWithFilters(
            @Param("portfolioId") UUID portfolioId,
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Filtered queries for account
    @Query("""
            SELECT t FROM TransactionEntity t WHERE t.account.id = :accountId
            AND (:transactionType IS NULL OR t.transactionType = :transactionType)
            AND (:startDate IS NULL OR t.transactionDate >= :startDate)
            AND (:endDate IS NULL OR t.transactionDate <= :endDate)
            """)
    public Page<TransactionEntity> findByAccountIdWithFilters(
            @Param("accountId") UUID accountId,
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // findWithFilterAndSymbols is absorbed into
    @Query("""
            SELECT t FROM TransactionEntity t
            WHERE (:portfolioId IS NULL OR t.portfolioId = :portfolioId)
            AND (:accountId IS NULL OR t.account.id = :accountId)
            AND (:transactionType IS NULL OR t.transactionType = :transactionType)
            AND (:startDate IS NULL OR t.transactionDate >= :startDate)
            AND (:endDate IS NULL OR t.transactionDate <= :endDate)
            AND (:symbols IS NULL OR t.primaryId IN :symbols) 
            ORDER BY t.transactionDate DESC
            """)
    public Page<TransactionEntity> findWithFilters(
            @Param("portfolioId") UUID portfolioId,
            @Param("accountId") UUID accountId,
            @Param("transactionType") TransactionType transactionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("symbols") Set<String> symbols,
            Pageable pageable);

}
