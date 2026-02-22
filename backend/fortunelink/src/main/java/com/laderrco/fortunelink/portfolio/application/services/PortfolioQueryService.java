package com.laderrco.fortunelink.portfolio.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;

import lombok.RequiredArgsConstructor;


// FOR PORTFOLIO SPECIFIC QUERIES ONLY, NO ACCCOUNT STUFF HERE

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioQueryService {
    private final PortfolioRepository portfolioRepository;

    private final MarketDataService marketDataService;
    private final ExchangeRateService exchangeRateService;
    private final PortfolioValuationService portfolioValuationService;

}
