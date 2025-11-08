package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class PerformanceCalculationService {
  public Percentage calculateTotalReturn(Portfolio portfolio, MarketDataService marketDataService) {
        Money currentValue = calculateCurrentValue(portfolio, marketDataService);
        Money totalInvested = calculateTotalInvested(portfolio);
        
        if (totalInvested.isZero()) {
            return Percentage.of(0);
        }
        
        Money gain = currentValue.subtract(totalInvested);
        return Percentage.of(gain.divide(totalInvested.amount()).amount());
    }
    
    public Money calculateRealizedGains(List<Transaction> transactions) {
        return transactions.stream()
            .filter(tx -> tx.getTransactionType() == TransactionType.SELL)
            .map(this::calculateSellGain)
            .reduce(Money.ZERO(ValidatedCurrency.USD), Money::add); // TODO: Handle multi-currency
    }
    
    public Money calculateUnrealizedGains(Portfolio portfolio, MarketDataService marketDataService) {
        return portfolio.getAccounts().stream()
            .flatMap(account -> account.getAssets().stream())
            .map(asset -> {
                Money currentPrice = marketDataService.getCurrentPrice(asset.getAssetIdentifier());
                Money currentValue = asset.calculateCurrentValue(currentPrice);
                return currentValue.subtract(asset.getCostBasis());
            })
            .reduce(Money.ZERO(ValidatedCurrency.USD), Money::add); // TODO: Handle multi-currency
    }
    
    public Percentage calculateTimeWeightedReturn(Portfolio portfolio) {
        // Complex calculation - left as exercise
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    private Money calculateCurrentValue(Portfolio portfolio, MarketDataService marketDataService) {
        // Delegate to ValuationService or implement here
        return Money.ZERO(ValidatedCurrency.USD);
    }
    
    private Money calculateTotalInvested(Portfolio portfolio) {
        return portfolio.getAccounts().stream()
            .flatMap(account -> account.getTransactions().stream())
            .filter(tx -> tx.getTransactionType() == TransactionType.BUY || tx.getTransactionType() == TransactionType.DEPOSIT)
            .map(tx -> tx.getTransactionType() == TransactionType.BUY 
                ? tx.calculateTotalCost() 
                : tx.getPricePerUnit()) // price per unit is amount depending on the transactiontype
            .reduce(Money.ZERO(ValidatedCurrency.USD), Money::add);
    }
    
    private Money calculateSellGain(Transaction sellTransaction) {
        // Simplified - would need to track cost basis properly
        return Money.ZERO(ValidatedCurrency.USD);
    }
}
