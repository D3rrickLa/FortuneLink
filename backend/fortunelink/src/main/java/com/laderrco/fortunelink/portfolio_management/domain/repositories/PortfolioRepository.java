package com.laderrco.fortunelink.portfolio_management.domain.repositories;

import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

// starting with Portfolio to house everything, BUT planning for separation of transaction 
public interface PortfolioRepository {
    Portfolio save(Portfolio portfolio);

    Optional<Portfolio> findById(PortfolioId id);

    Optional<Portfolio> findByUserId(UserId userId);

    // This query ensures the record MUST belong to the user to be returned
    Optional<Portfolio> findByIdAndUserId(PortfolioId id, UserId userId);

    List<Portfolio> findAllByUserId(UserId userId);

    Long countByUserId(UserId userId);

    void delete(PortfolioId id);
}
