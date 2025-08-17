package com.laderrco.fortunelink.portfoliomanagment.domain.repositories;

import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;

public interface PortfolioRepository {
    public PortfolioId save(Portfolio portfolio);
    public Optional<PortfolioId> findById(PortfolioId portfolioId);
    public List<Portfolio> findAll(UserId userId);
    public void delete(PortfolioId portfolioId);
}
