package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate; // Added this import
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries.TransactionQuery;

@Repository
public interface JpaTransactionRepository
        extends JpaRepository<TransactionEntity, UUID>, JpaSpecificationExecutor<TransactionEntity> {

    // --- Existing Queries ---

    List<TransactionEntity> findByPortfolioId(UUID portfolioId, Pageable pageable);

    Page<TransactionEntity> findByAccountId(UUID accountId, Pageable pageable);

    long countByPortfolioId(UUID portfolioId);

    long countByAccountId(UUID accountId);

    @Query("SELECT t FROM TransactionEntity t WHERE t.portfolioId = :portfolioId " +
            "AND (:transactionType IS NULL OR t.transactionType = :transactionType) " +
            "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
            "AND (:endDate IS NULL OR t.transactionDate <= :endDate)")
    Page<TransactionEntity> findByPortfolioIdWithFilters(
            @Param("portfolioId") UUID portfolioId,
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("""
            SELECT t FROM TransactionEntity t WHERE t.account.id = :accountId
            AND (:transactionType IS NULL OR t.transactionType = :transactionType)
            AND (:startDate IS NULL OR t.transactionDate >= :startDate)
            AND (:endDate IS NULL OR t.transactionDate <= :endDate)
            """)
    Page<TransactionEntity> findByAccountIdWithFilters(
            @Param("accountId") UUID accountId,
            @Param("transactionType") String transactionType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // This is the one causing the %7 error - we'll keep it for reference but use Specs for the implementation
    @Query("""
            SELECT t FROM TransactionEntity t
            WHERE (:portfolioId IS NULL OR t.portfolioId = :portfolioId)
              AND (:accountId IS NULL OR t.account.id = :accountId)
              AND (:transactionType IS NULL OR t.transactionType = :transactionType)
              AND (:startDate IS NULL OR t.transactionDate >= :startDate)
              AND (:endDate IS NULL OR t.transactionDate <= :endDate)
              AND (:#{#symbols == null} = true OR t.primaryId IN :symbols)
            """)
    Page<TransactionEntity> findWithFilters(
            @Param("portfolioId") UUID portfolioId,
            @Param("accountId") UUID accountId,
            @Param("transactionType") String transactionType,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("symbols") List<String> symbols,
            Pageable pageable);

    // --- Specifications Helper ---

    /**
     * Specification inner class to handle dynamic filtering.
     * This avoids the PostgreSQL "could not determine data type" error by only 
     * adding parameters to the query that are actually present.
     */
    class TransactionSpecifications {

        public static Specification<TransactionEntity> withFilters(TransactionQuery query) {
            return (root, cq, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                if (query.portfolioId() != null) {
                    predicates.add(cb.equal(root.get("portfolioId"), query.portfolioId()));
                }

                if (query.accountId() != null) {
                    predicates.add(cb.equal(root.get("account").get("id"), query.accountId()));
                }

                if (query.transactionType() != null) {
                    // Assuming transactionType is an Enum in the Query object
                    predicates.add(cb.equal(root.get("transactionType"), query.transactionType().name()));
                }

                if (query.startInstant() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), query.startInstant()));
                }

                if (query.endInstant() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), query.endInstant()));
                }

                if (query.assetSymbols() != null && !query.assetSymbols().isEmpty()) {
                    predicates.add(root.get("primaryId").in(query.assetSymbols()));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };
        }
    }
}