package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository {
    Portfolio save(Portfolio portfolio);

    Optional<Portfolio> findById(PortfolioId id);

    /**
     * Returns the single portfolio for a given user. DESIGN CONSTRAINT: FortuneLink
     * currently
     * supports exactly one portfolio per user. If multi-portfolio support is added,
     * this method
     * signature must change to List<Portfolio>.
     */
    Optional<Portfolio> findByUserId(UserId userId);

    /**
     * Use this when you need the full aggregate (e.g., recording a transaction).
     */
    Optional<Portfolio> findByIdAndUserId(PortfolioId id, UserId userId);

    List<Portfolio> findAllByUserId(UserId userId);

    /**
     * Counts ACTIVE (non-deleted) portfolios for a user.
     *
     * Bug 13 fix: Implementations MUST exclude soft-deleted portfolios from this
     * count (WHERE
     * deleted = false). If soft-deleted portfolios are counted, a user who deletes
     * their portfolio
     * to start fresh will be permanently locked out of creating a new one.
     *
     * JPA implementation example: @Query("SELECT COUNT(p) FROM Portfolio p WHERE
     * p.userId = :userId
     * AND p.deleted = false") Long countActiveByUserId(@Param("userId") UserId
     * userId);
     */
    Long countByUserId(UserId userId);

    boolean exists(PortfolioId portfolioId);

    /**
     * Lightweight ownership check - no aggregate load.
     * <p>
     * Use this wherever you only need to verify ownership without needing the
     * aggregate itself
     * (e.g., read-only query services validating access). Use findByIdAndUserId()
     * when you actually
     * need the Portfolio for mutation.
     */
    boolean existsByIdAndUserId(PortfolioId id, UserId userId);

    /**
     * Returns only ACTIVE (non-deleted) portfolios for a user.
     * Deleted portfolios are invisible to the application layer — filtering
     * belongs here, not in service code.
     */
    List<Portfolio> findAllActiveByUserId(UserId userId);

    void delete(PortfolioId id);
}
