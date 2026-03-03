package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository {
    Portfolio save(Portfolio portfolio);

    Optional<Portfolio> findById(PortfolioId id);

    Optional<Portfolio> findByUserId(UserId userId);

    /**
     * Use this when you need the full aggregate (e.g., recording a transaction).
     */
    Optional<Portfolio> findByIdAndUserId(PortfolioId id, UserId userId);

    List<Portfolio> findAllByUserId(UserId userId);

    /*
    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.recalculationFailed = true WHERE a.accountId = :id")
     */
    void markAccountAsStale(AccountId accountId);

    /*
    @Modifying
    @Transactional
    @Query("UPDATE Account a SET a.recalculationFailed = false WHERE a.accountId = :id")
     */
    void clearStaleFlag(AccountId accountId);

    Long countByUserId(UserId userId);

    boolean exists(PortfolioId portfolioId);

    /**
     * Lightweight ownership check - no aggregate load.
     * <p>
     * Use this wherever you only need to verify ownership without needing the
     * aggregate itself (e.g., read-only query services validating access).
     * Use findByIdAndUserId() when you actually need the Portfolio for mutation.
     */
    boolean existsByIdAndUserId(PortfolioId id, UserId userId);

    void delete(PortfolioId id);
}