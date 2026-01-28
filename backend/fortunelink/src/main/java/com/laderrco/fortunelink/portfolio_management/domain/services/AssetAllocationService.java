package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// So the reason why we had to change from percentage to money is just because of rounding and precision
public class AssetAllocationService implements ClassValidation {
    private final PortfolioValuationService valuationService;
    private final MarketDataService marketDataService;
    
    // Constructor injection - no need to pass marketDataService into every method anymore
    public AssetAllocationService(PortfolioValuationService valuationService, MarketDataService marketDataService) {
        this.valuationService = valuationService;
        this.marketDataService = marketDataService;
    }
    
    public Map<AssetType, Money> calculateAllocationByType(Portfolio portfolio, Instant asOfDate) {
        validate(portfolio, asOfDate);
        // Use portfolio preference for a unified report in one currency
        ValidatedCurrency reportingCurrency = portfolio.getPortfolioCurrencyPreference();
        Map<AssetType, Money> valueByType = new HashMap<>();
        
        portfolio.getAccounts().forEach(account -> {
            account.getAssets().forEach(asset -> {
                // Convert to reporting currency so values are comparable/addable
                Money assetValue = valuationService.calculateAssetValue(asset, reportingCurrency, asOfDate);
                // Only add to the map if the value is greater than zero
                if (assetValue.amount().compareTo(BigDecimal.ZERO) > 0) {
                    valueByType.merge(asset.getAssetIdentifier().getAssetType(), assetValue, Money::add);
                }
            });
        });
        
        return valueByType;
    }

    public Map<AccountType, Money> calculateAllocationByAccount(Portfolio portfolio, Instant asOfDate) {
        validate(portfolio, asOfDate);
        ValidatedCurrency reportingCurrency = portfolio.getPortfolioCurrencyPreference();
        Map<AccountType, Money> valueByAccount = new HashMap<>();
        
        portfolio.getAccounts().forEach(account -> {
            // Calculate total account value in the unified reporting currency
            Money accountValue = valuationService.calculateAccountValue(account, reportingCurrency, asOfDate);
            valueByAccount.merge(account.getAccountType(), accountValue, Money::add);
        });
        
        return valueByAccount;
    }

    public Map<ValidatedCurrency, Money> calculateAllocationByCurrency(Portfolio portfolio, Instant asOfDate) {
        validate(portfolio, asOfDate);
        Map<ValidatedCurrency, Money> valueByCurrency = new HashMap<>();

        for (Account account : portfolio.getAccounts()) {
            // 1. Cash is already in its native currency
            Money cash = account.getCashBalance();
            valueByCurrency.merge(cash.currency(), cash, Money::add);

            // 2. For true currency allocation, we need the asset's NATIVE currency
            for (Asset asset : account.getAssets()) {
                // Get the currency the asset is actually priced in (e.g., USD for AAPL)
                ValidatedCurrency nativeAssetCurrency = marketDataService.getTradingCurrency(asset.getAssetIdentifier());
                
                // IMPORTANT: Calculate value in its OWN currency, not the account base
                Money assetValue = valuationService.calculateAssetValue(asset, nativeAssetCurrency, asOfDate); 
                valueByCurrency.merge(nativeAssetCurrency, assetValue, Money::add);
            }
        }

        return valueByCurrency;
    } 

    private void validate(Portfolio portfolio, Instant asOfDate) {
        ClassValidation.validateParameter(portfolio);
        ClassValidation.validateParameter(asOfDate);
    }
}
