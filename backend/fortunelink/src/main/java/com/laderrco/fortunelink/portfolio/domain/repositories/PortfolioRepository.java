package com.laderrco.fortunelink.portfolio.domain.repositories;

import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public interface PortfolioRepository {
    Portfolio save(Portfolio portfolio);

    Optional<Portfolio> findById(PortfolioId id);

    Optional<Portfolio> findByUserId(UserId userId);

    /**
     * Ownership-scoped fetch — record is only returned if it belongs to userId.
     * Use this when you need the full aggregate (e.g., recording a transaction).
     */
    Optional<Portfolio> findByIdAndUserId(PortfolioId id, UserId userId);

    List<Portfolio> findAllByUserId(UserId userId);

    Long countByUserId(UserId userId);

    boolean exists(PortfolioId portfolioId);

    /**
     * Lightweight ownership check — no aggregate load.
     *
     * Issue 8 fix: TransactionQueryService.validateOwnership() was calling
     * findByIdAndUserId() and discarding the returned Portfolio, loading the
     * entire aggregate just to verify the user owns it. That's wasteful on every
     * paginated transaction history request.
     *
     * Use this wherever you only need to verify ownership without needing the
     * aggregate itself (e.g., read-only query services validating access).
     * Use findByIdAndUserId() when you actually need the Portfolio for mutation.
     */
    boolean existsByIdAndUserId(PortfolioId id, UserId userId);

    void delete(PortfolioId id);
}