package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.ErrorType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetQuote;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// ANSWERS THE QUESTION: WHAT IS IT WORTH NOW (STATELESS AND REUSABLE)
public class PortfolioValuationService {
    private final MarketDataService marketDataService;
    private final ExchangeRateService exchangeRateService;

    // Constructor Injection
    public PortfolioValuationService(MarketDataService marketDataService, ExchangeRateService exchangeRateService) {
        this.marketDataService = marketDataService;
        this.exchangeRateService = exchangeRateService;
    }

    // Main entry point - handles both live and historical data, calculates on the
    // portfolio level
    public Money calculateTotalValue(Portfolio portfolio, Instant asOfDate) {
        ValidatedCurrency pref = portfolio.getPortfolioCurrencyPreference();
        return portfolio.getAccounts().stream()
                .map(account -> calculateAccountValue(account, pref, asOfDate))
                .reduce(Money.ZERO(portfolio.getPortfolioCurrencyPreference()), Money::add);
    }

    public Money calculateAccountValue(Account account, ValidatedCurrency targetCurrency, Instant asOfDate) {
        Money cashValue = account.getCashBalance().currency().equals(targetCurrency)
                ? account.getCashBalance()
                : exchangeRateService.convert(account.getCashBalance(), targetCurrency, asOfDate);

        Money assetsValue = account.getAssets().stream()
                .map(asset -> calculateAssetValue(asset, targetCurrency, asOfDate))
                .reduce(Money.ZERO(targetCurrency), Money::add);

        return cashValue.add(assetsValue);
    }

    public Money calculateAssetValue(Asset asset, ValidatedCurrency targetCurrency, Instant asOfDate) {
        MarketAssetQuote quote;

        // Determine whether to use current or historical price
        if (asOfDate == null || !asOfDate.isBefore(Instant.now().minusSeconds(5L))) {
            quote = marketDataService.getCurrentQuote(asset.getAssetIdentifier())
                .orElseThrow(() -> new MarketDataException(
                    "Current price unavailable for asset: " + asset.getAssetIdentifier(), ErrorType.DATA_UNAVAILABLE));
        } else {
            LocalDateTime ldt = LocalDateTime.ofInstant(asOfDate, ZoneOffset.UTC);
            quote = marketDataService.getHistoricalQuote(asset.getAssetIdentifier(), ldt)
                .orElseThrow(() -> new MarketDataException(
                    "Historical price unavailable for asset: " + asset.getAssetIdentifier() +" at " + asOfDate, ErrorType.DATA_UNAVAILABLE));
        }

        // Multiply price by quantity to get value in asset's currency
        Money localValue = quote.currentPrice().multiply(asset.getQuantity());

        // Convert to target currency if needed
        if (!localValue.currency().equals(targetCurrency)) {
            localValue = exchangeRateService.convert(localValue, targetCurrency, asOfDate);
        }

        return localValue;
    }

}
