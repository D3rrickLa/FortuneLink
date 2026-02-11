package com.laderrco.fortunelink.portfolio_management.domain.repositories;

import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;

public interface PortfolioRepository {
    Portfolio save(Portfolio portfolio);

    Optional<Portfolio> findById(PortfolioId id);

    Optional<Portfolio> findByUserId(UserId userId);

    // This query ensures the record MUST belong to the user to be returned
    Optional<Portfolio> findByIdAndUserId(PortfolioId id, UserId userId);

    List<Portfolio> findAllByUserId(UserId userId);

    Long countByUserId(UserId userId);

    boolean exists(PortfolioId portfolioId);

    void delete(PortfolioId id);
}