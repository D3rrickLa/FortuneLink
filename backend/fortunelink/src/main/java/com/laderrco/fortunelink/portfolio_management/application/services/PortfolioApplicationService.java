package com.laderrco.fortunelink.portfolio_management.application.services;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
@AllArgsConstructor
@Data
/*
 * Orchestrates use cases and coordinates between domain services, repositories, and infrastructure
 */
public class PortfolioApplicationService {
    // use case handler
    private final PortfolioRepository portfolioRepository;
    private final PortfolioValuationService portfolioValuationService;
    private final MarketDataService marketDataService;

    /*
     * createPortfolio
     * addAccount
     * recordTransaction
     * getPortfolioSummary
     * getTRansactionHistory
     */
}
