package com.laderrco.fortunelink.portfolio_management.domain.repositories;

import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

// starting with Portfolio to house everything, BUT planning for separation of transaction 
public interface PortfolioRepository {
    Portfolio save(Portfolio portfolio);
    Optional<Portfolio> findById(PortfolioId id);
    Optional<Portfolio> findByUserId(UserId userId);
    void delete(PortfolioId id);
}
