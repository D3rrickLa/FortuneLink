package com.laderrco.fortunelink.PortfolioManagement.domain.Repositories;

import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Portfolio;

public interface PortfolioRepository {
    Optional<Portfolio> findById(UUID id);

    Portfolio savePortfolio(Portfolio portfolio);
}
