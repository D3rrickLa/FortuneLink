package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

// TODO: add @Service to all services
public class AssetAllocationService {
    private final PortfolioValuationService valuationService;
    
    public AssetAllocationService(PortfolioValuationService valuationService) {
        this.valuationService = valuationService;
    }

    public Map<AssetType, Percentage> calculateAllocationByType(
        Portfolio portfolio, 
        MarketDataService marketDataService
    ) {
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService);
        
        if (totalValue.isZero()) {
            return Map.of();
        }
        
        Map<AssetType, Money> valueByType = new HashMap<>();
        
        portfolio.getAccounts().stream()
            .flatMap(account -> account.getAssets().stream())
            .forEach(asset -> {
                Money assetValue = valuationService.calculateAssetValue(asset, marketDataService);
                valueByType.merge(asset.getAssetIdentifier().getAssetType(), assetValue, Money::add);
            });
        
        // Convert to percentages
        Map<AssetType, Percentage> allocation = new HashMap<>();
        valueByType.forEach((type, value) -> {
            Percentage percentage = value.divide(totalValue.amount()).toPercentage();
            allocation.put(type, percentage);
        });
        
        return allocation;
    }
    
    public Map<AccountType, Percentage> calculateAllocationByAccount(
        Portfolio portfolio, 
        MarketDataService marketDataService
    ) {
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService);
        
        if (totalValue.isZero()) {
            return Map.of();
        }
        
        Map<AccountType, Money> valueByAccount = new HashMap<>();
        
        portfolio.getAccounts().forEach(account -> {
            Money accountValue = valuationService.calculateAccountValue(account, marketDataService);
            valueByAccount.merge(account.getAccountType(), accountValue, Money::add);
        });
        
        Map<AccountType, Percentage> allocation = new HashMap<>();
        valueByAccount.forEach((type, value) -> {
            Percentage percentage = value.divide(totalValue.amount()).toPercentage();
            allocation.put(type, percentage);
        });
        
        return allocation;
    }
    
    public Map<ValidatedCurrency, Percentage> calculateAllocationByCurrency(
        Portfolio portfolio, 
        MarketDataService marketDataService
    ) {
        // Similar pattern - group by currency
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService);
        
        if (totalValue.isZero()) {
            return Map.of();
        }
        
        Map<ValidatedCurrency, Money> valueByCurrency = new HashMap<>();
        
        portfolio.getAccounts().forEach(account -> {
            Money accountValue = valuationService.calculateAccountValue(account, marketDataService);
            valueByCurrency.merge(account.getBaseCurrency(), accountValue, Money::add);
        });
        
        Map<ValidatedCurrency, Percentage> allocation = new HashMap<>();
        valueByCurrency.forEach((currency, value) -> {
            // Convert to portfolio base currency first if needed
            Percentage percentage = value.divide(totalValue.amount()).toPercentage();
            allocation.put(currency, percentage);
        });
        
        return allocation;
    }   
}
