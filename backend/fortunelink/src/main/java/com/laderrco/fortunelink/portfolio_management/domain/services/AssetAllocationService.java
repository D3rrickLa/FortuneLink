package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AssetAllocationService {
    private final PortfolioValuationService valuationService;
    
    public Map<AssetType, Money> calculateAllocationByType(Portfolio portfolio, MarketDataService marketDataService, Instant asOfDate) {
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
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService, asOfDate);
        
        if (totalValue.isZero()) {
            return Map.of();
        }
        
        Map<AccountType, Money> valueByAccount = new HashMap<>();

        portfolio.getAccounts().forEach(account -> {
            Money accountValue = valuationService.calculateAccountValue(account, marketDataService, asOfDate);
            valueByAccount.merge(account.getAccountType(), accountValue, Money::add);        
        });
        
        return valueByAccount;
    }
    
    public Map<ValidatedCurrency, Money> calculateAllocationByCurrency(Portfolio portfolio, MarketDataService marketDataService, Instant asOfDate) {
        Money totalValue = valuationService.calculateTotalValue(portfolio, marketDataService, asOfDate);
        
        if (totalValue.isZero()) {
            return Map.of();
        }
        
        Map<ValidatedCurrency, Money> valueByCurrency = new HashMap<>();
        
        portfolio.getAccounts().forEach(account -> {
            Money accountValue = valuationService.calculateAccountValue(account, marketDataService, asOfDate);
            valueByCurrency.merge(account.getBaseCurrency(), accountValue, Money::add);
        });
        
        
        return valueByCurrency;
    }   

    // private Money getPriceForDate(MarketDataService marketDataService, AssetIdentifier id, Instant asOfDate) {
    //     if (asOfDate == null || !asOfDate.isBefore(Instant.now().minusSeconds(10))) {
    //         return marketDataService.getCurrentPrice(id);
    //     }
    //     return marketDataService.getHistoricalPrice(id, LocalDateTime.ofInstant(asOfDate, ZoneOffset.UTC));
    // }
}
