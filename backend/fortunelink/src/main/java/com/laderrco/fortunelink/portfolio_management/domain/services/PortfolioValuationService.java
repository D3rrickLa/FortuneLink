package com.laderrco.fortunelink.portfolio_management.domain.services;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// @Service
// ANSWERS THE QUESTION: WHAT IS IT WORTH NOW (STATELESS AND REUSABLE)
public class PortfolioValuationService {
    public Money calculateTotalValue(Portfolio portfolio, MarketDataService marketDataService) {
        return portfolio.getAccounts().stream()
            .map(account -> calculateAccountValue(account, marketDataService))
            .reduce(Money.ZERO(portfolio.getPortfolioCurrency()), Money::add);
    }

    public Money calculateAccountValue(Account account, MarketDataService marketDataService) {
         // Cash balance
        Money cashValue = account.getCashBalance();
        
        // Asset values
        Money assetsValue = account.getAssets().stream()
            .map(asset -> calculateAssetValue(asset, marketDataService))
            .reduce(Money.ZERO(account.getBaseCurrency()), Money::add);
        
        return cashValue.add(assetsValue);
    }

    public Money calculateAssetValue(Asset asset, MarketDataService marketDataService) {
        Money currentPrice = marketDataService.getCurrentPrice(asset.getAssetIdentifier());
        return currentPrice.multiply(asset.getQuantity());
    }

    public Money calculateTotalAssets(Portfolio portfolio, MarketDataService marketDataService) {
        // Similar to calculateTotalValue but might exclude liabilities
        return calculateTotalValue(portfolio, marketDataService);
    }
}
