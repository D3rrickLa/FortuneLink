package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

// @Service
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
    
    public Money calculateRealizedGains(Portfolio portfolio, List<Transaction> transactions) {
        ValidatedCurrency portfolioBaseCurrency = portfolio.getPortfolioCurrency();
        return transactions.stream()
            .filter(tx -> tx.getTransactionType() == TransactionType.SELL)
            .map(tx -> calculateSellGain(tx, portfolio))
            .reduce(Money.ZERO(portfolioBaseCurrency), Money::add);
    }
    
    public Money calculateUnrealizedGains(Portfolio portfolio, MarketDataService marketDataService) {
        ValidatedCurrency baseCurrency = portfolio.getPortfolioCurrency();
        
        return portfolio.getAccounts().stream()
            .flatMap(account -> account.getAssets().stream())
            .map(asset -> {
                Money currentPrice = marketDataService.getCurrentPrice(asset.getAssetIdentifier());
                Money currentValue = asset.calculateCurrentValue(currentPrice);
                return currentValue.subtract(asset.getCostBasis());
            })
            .reduce(Money.ZERO(baseCurrency), Money::add);
    }
    
    public Percentage calculateTimeWeightedReturn(Portfolio portfolio) {
        // Complex calculation - left as exercise/ as needed
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    private Money calculateCurrentValue(Portfolio portfolio, MarketDataService marketDataService) {
        // Delegate to ValuationService or implement here
        // we are going to use the PortfolioEvaluationService.java
        PortfolioValuationService portfolioValuationService = new PortfolioValuationService();
        return portfolioValuationService.calculateTotalValue(portfolio, marketDataService);
        // return Money.ZERO(ValidatedCurrency.USD);
    }
    
    private Money calculateTotalInvested(Portfolio portfolio) {
        ValidatedCurrency baseCurrency = portfolio.getPortfolioCurrency();
        
        Money totalDeposits = portfolio.getAccounts().stream()
            .flatMap(account -> account.getTransactions().stream())
            .filter(tx -> tx.getTransactionType() == TransactionType.BUY || 
                         tx.getTransactionType() == TransactionType.DEPOSIT)
            .map(tx -> tx.getTransactionType() == TransactionType.BUY 
                ? tx.calculateTotalCost() 
                : tx.getPricePerUnit())
            .reduce(Money.ZERO(baseCurrency), Money::add);
        
        Money totalWithdrawals = portfolio.getAccounts().stream()
            .flatMap(account -> account.getTransactions().stream())
            .filter(tx -> tx.getTransactionType() == TransactionType.WITHDRAWAL)
            .map(Transaction::getPricePerUnit)
            .reduce(Money.ZERO(baseCurrency), Money::add);
        
        return totalDeposits.subtract(totalWithdrawals);
    }
    
    // this uses average cost basis method - simplest for MVP (price * quantity - fees)
    // returns proceeds - cost basis
    // some proboems: not accurate for tax purposes (need specific lot tracking) and doens't handle partia lsales optimally
    // this should become adjusted cost basis?
    private Money calculateSellGain(Transaction sellTransaction, Portfolio portfolio) {
        // Sale proceeds (price * quantity - fees)
        Money saleProceeds = sellTransaction.calculateNetAmount();
        
        // Get the asset to find its average cost basis
        Account account = portfolio.getAccount(
            portfolio.getAccounts().stream()
                .filter(ac -> ac.getTransactions().stream()
                    .anyMatch(t -> t.getTransacationId().equals(sellTransaction.getTransacationId())))
                .map(Account::getAccountId)
                .findFirst()
                .orElse(null)
        ); // need to stream where account has this id
        Asset asset = account.getAsset(sellTransaction.getAssetIdentifier());
        
        // Calculate cost basis for the quantity sold
        // Using simple average cost method for MVP
        Money averageCostPerUnit = asset.getCostBasis().divide(asset.getQuantity());
        Money costBasisForSale = averageCostPerUnit.multiply(sellTransaction.getQuantity());
        
        // Realized gain = proceeds - cost basis
        return saleProceeds.subtract(costBasisForSale);
    }
}
