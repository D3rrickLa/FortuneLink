package com.laderrco.fortunelink.portfoliomanagment.repositories;

import java.util.Optional;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;

public interface PortfolioRepository {
    public PortfolioId save(Portfolio portfolio);
    public Optional<PortfolioId> findById(PortfolioId portfolioId);
}
