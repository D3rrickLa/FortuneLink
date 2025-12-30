package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// ANSWERS THE QUESTION: WHAT IS IT WORTH NOW (STATELESS AND REUSABLE)
public class PortfolioValuationService {
    // Main entry point - handles both live and historical data, calculates on the portfolio level
    public Money calculateTotalValue(Portfolio portfolio, MarketDataService marketDataService, Instant asOfDate) {
        return portfolio.getAccounts().stream()
            .map(account -> calculateAccountValue(account, marketDataService, asOfDate))
            .reduce(Money.ZERO(portfolio.getPortfolioCurrencyPreference()), Money::add);
    }

    public Money calculateAccountValue(Account account, MarketDataService marketDataService, Instant asOfDate) {
        Money cashBalance = account.getCashBalance();

        Money assetsValue = account.getAssets().stream()
            .map(asset -> calculateAssetValue(asset, marketDataService, asOfDate))
            .reduce(Money.ZERO(account.getBaseCurrency()), Money::add);
        
        return cashBalance.add(assetsValue);
    }

    public Money calculateAssetValue(Asset asset, MarketDataService marketDataService, Instant asOfDate) {
        Money price;
        if (asOfDate == null || !asOfDate.isBefore(Instant.now().minusSeconds(5L))) {
            price = marketDataService.getCurrentPrice(asset.getAssetIdentifier());
        } 
        else {
            LocalDateTime ldt = LocalDateTime.ofInstant(asOfDate, ZoneOffset.UTC);
            price = marketDataService.getHistoricalPrice(asset.getAssetIdentifier(), ldt);
        }

        return price.multiply(asset.getQuantity());
    }

}
