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
    // public Money calculateTotalValue(Portfolio portfolio, MarketDataService marketDataService) {
    //     return portfolio.getAccounts().stream()
    //         .map(account -> calculateAccountValue(account, marketDataService))
    //         .reduce(Money.ZERO(portfolio.getPortfolioCurrencyPreference()), Money::add);
    // }

    // public Money calculateAccountValue(Account account, MarketDataService marketDataService) {
    //      // Cash balance
    //     Money cashValue = account.getCashBalance();
        
    //     // Asset values
    //     Money assetsValue = account.getAssets().stream()
    //         .map(asset -> calculateAssetValue(asset, marketDataService))
    //         .reduce(Money.ZERO(account.getBaseCurrency()), Money::add);
        
    //     return cashValue.add(assetsValue);
    // }

    // public Money calculateAssetValue(Asset asset, MarketDataService marketDataService) {
    //     Money currentPrice = marketDataService.getCurrentPrice(asset.getAssetIdentifier());
    //     return currentPrice.multiply(asset.getQuantity());
    // }

    // ON THIS POINT, WE ARE ONLY CALCULATING THE VALUE OF THE ASSETS IN THE PORTFOLIO NOT THE LIABILITIES. THAT
    // IS HANDELD IN THE QUERY SERVICE LAYER WILL IT WILL INTERACT WITH SAID DOMAIN
    // // honestly think this is wrong... because this really does nothing
    // public Money calculateTotalAssets(Portfolio portfolio, MarketDataService marketDataService) {
    //     // Similar to calculateTotalValue but might exclude liabilities
    //     return calculateTotalValue(portfolio, marketDataService);
    // }

    // public Money calculateHistoricalValue(Portfolio portfolio, MarketDataService marketDataService,
    //         Instant calculationDate) {
    //     // TODO Auto-generated method stub
    //     throw new UnsupportedOperationException("Unimplemented method 'calculateHistoricalValue'");
    // }

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
