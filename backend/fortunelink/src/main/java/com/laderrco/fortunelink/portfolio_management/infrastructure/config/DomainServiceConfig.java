package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.laderrco.fortunelink.portfolio_management.domain.services.AssetAllocationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PerformanceCalculationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;

@Configuration
public class DomainServiceConfig {
    
    @Bean
    public PortfolioValuationService portfolioValuationService (MarketDataService marketDataService, ExchangeRateService exchangeRateService) {
        return new PortfolioValuationService(marketDataService, exchangeRateService);
    }

    @Bean
    public PerformanceCalculationService performanceCalculationService() {
        return new PerformanceCalculationService();
    }

    @Bean
    public AssetAllocationService assetAllocationService(PortfolioValuationService portfolioValuationService, MarketDataService marketDataService) {
        return new AssetAllocationService(portfolioValuationService, marketDataService);
    }

}