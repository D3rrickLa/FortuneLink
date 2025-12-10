package com.laderrco.fortunelink.portfolio_management.application.mappers;

import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.AssetResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// convertes between portfolio domain entity and DTOs, handles ocmplex nested conversions
public class PortfolioMapper {
    public PortfolioResponse toResponse(Portfolio portfolio, MarketDataService marketDataService) {
        return null;
    }

    public AccountResponse toAccountResponse(Account account, MarketDataService marketDataService) {
        return null;
    }

    public AssetResponse toAssetResponse (Asset asset, Money price) {
        return null;
    }
    
}