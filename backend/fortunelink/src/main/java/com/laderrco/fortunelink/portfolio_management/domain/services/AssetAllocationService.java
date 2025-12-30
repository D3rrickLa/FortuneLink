package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import lombok.AllArgsConstructor;

// So the reason why we had to change from percentage to money is just because of rounding and precision
@AllArgsConstructor
public class AssetAllocationService implements ClassValidation {
    private final PortfolioValuationService valuationService;
    
    public Map<AssetType, Money> calculateAllocationByType(Portfolio portfolio, MarketDataService marketDataService, Instant asOfDate) {
        validate(portfolio, marketDataService, asOfDate);
        Map<AssetType, Money> valueByType = new HashMap<>();
        
        portfolio.getAccounts().stream()
        .flatMap(account -> account.getAssets().stream())
        .forEach(asset -> {
            Money assetValue = valuationService.calculateAssetValue(asset, marketDataService, asOfDate);
            valueByType.merge(asset.getAssetIdentifier().getAssetType(), assetValue, Money::add);
        });
        
        return valueByType;
    }
    
    public Map<AccountType, Money> calculateAllocationByAccount(Portfolio portfolio, MarketDataService marketDataService, Instant asOfDate) {
        validate(portfolio, marketDataService, asOfDate);
        Map<AccountType, Money> valueByAccount = new HashMap<>();
        
        portfolio.getAccounts().forEach(account -> {
            Money accountValue = valuationService.calculateAccountValue(account, marketDataService, asOfDate);
            valueByAccount.merge(account.getAccountType(), accountValue, Money::add);
        });
        
        return valueByAccount;
    }
    
    public Map<ValidatedCurrency, Money> calculateAllocationByCurrency(Portfolio portfolio, MarketDataService marketDataService, Instant asOfDate) {
        validate(portfolio, marketDataService, asOfDate);
        Map<ValidatedCurrency, Money> valueByCurrency = new HashMap<>();

        portfolio.getAccounts().forEach(account -> {
            Money accountValue = valuationService.calculateAccountValue(account, marketDataService, asOfDate);
            // This assumes accountValue is already converted to the portfolio's base currency 
            // OR that you are grouping by the account's primary currency.
            // TODO ^ above we have to impelement this
            valueByCurrency.merge(account.getBaseCurrency(), accountValue, Money::add);
        });

        return valueByCurrency;
    }   

    private void validate(Portfolio portfolio, MarketDataService marketDataService, Instant asOfDate) {
        ClassValidation.validateParameter(portfolio);
        ClassValidation.validateParameter(marketDataService);
        ClassValidation.validateParameter(asOfDate);
    }
}
