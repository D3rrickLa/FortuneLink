package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

// @Service
// ANSWERS THE QUESTION: HOW IS IT PERFORMING (COMPLEX ,USES VALUATION + HISTORY)
public class PerformanceCalculationService {
    /**
     * Calculates the total return percentage of the portfolio.
     * Formula: (Current Value - Total Invested) / Total Invested × 100
     * 
     * Example: Invested $10,000, now worth $12,000 = 20% return
     * 
     * @return Percentage representing total portfolio return (e.g., 20% gain or -5%
     *         loss)
     */
    public Percentage calculateTotalReturn(Portfolio portfolio, MarketDataService marketDataService) {
        Money currentValue = calculateCurrentValue(portfolio, marketDataService);
        Money totalInvested = calculateTotalInvested(portfolio);

        if (totalInvested.isZero()) {
            return Percentage.of(0);
        }

        Money gain = currentValue.subtract(totalInvested);
        return Percentage.of(gain.divide(totalInvested.amount()).amount());
    }

    /**
     * Calculates total realized gains (or losses) from all completed sales.
     * Realized gains = money you've actually locked in by selling assets.
     * 
     * Only includes SELL transactions. Uses simple average cost method for MVP.
     * Note: Does NOT use proper Canadian ACB calculation (future enhancement).
     * 
     * Example: Bought 100 shares at $10, sold 50 at $15 = $250 realized gain
     * 
     * @param transactions List of all transactions to analyze
     * @return Total money gained (or lost) from all sales in portfolio base
     *         currency
     */
    public Money calculateRealizedGains(Portfolio portfolio, List<Transaction> transactions)
            throws AccountNotFoundException {
        ValidatedCurrency portfolioBaseCurrency = portfolio.getPortfolioCurrency();

        Map<AssetIdentifier, List<Transaction>> txByAsset = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getAssetIdentifier));

        Money totalRealizedGains = Money.ZERO(portfolioBaseCurrency);

        for (Map.Entry<AssetIdentifier, List<Transaction>> entry : txByAsset.entrySet()) {
            List<Transaction> assetTxs = entry.getValue().stream()
                    .sorted(Comparator.comparing(Transaction::getTransactionDate))
                    .toList();

            Money assetGains = calculateFifoGainsForAsset(assetTxs, portfolioBaseCurrency);
            totalRealizedGains = totalRealizedGains.add(assetGains);
        }

        return totalRealizedGains;

    }

    /**
     * Calculates total unrealized gains (or losses) across all current holdings.
     * Unrealized gains = paper gains you haven't locked in yet (still holding the
     * asset).
     * 
     * Formula: (Current Market Value - Cost Basis) for each asset
     * 
     * Example: Own 100 shares, paid $1,000 total, now worth $1,200 = $200
     * unrealized gain
     * 
     * @return Total paper gains/losses across all assets in portfolio base currency
     */
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

    /**
     * Calculates time-weighted return (TWR) - a performance metric that eliminates
     * the impact of deposits/withdrawals timing.
     * 
     * TWR is useful for comparing portfolio performance to benchmarks because it
     * shows
     * how well your investments performed, independent of when you added/removed
     * money.
     * 
     * Not implemented for MVP - complex calculation requiring period segmentation.
     * 
     * @throws UnsupportedOperationException Always throws (not yet implemented)
     */
    public Percentage calculateTimeWeightedReturn(Portfolio portfolio) {
        // Complex calculation - left as exercise/ as needed
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private Money calculateCurrentValue(Portfolio portfolio, MarketDataService marketDataService) {
        // we are going to use the PortfolioEvaluationService.java for calculation
        // rather than a direct implementation
        PortfolioValuationService portfolioValuationService = new PortfolioValuationService();
        return portfolioValuationService.calculateTotalValue(portfolio, marketDataService);
    }

    /**
     * Helper: Calculates the total amount of money invested into the portfolio.
     * 
     * Formula: (All BUYs + All DEPOSITs) - (All WITHDRAWALs)
     * 
     * This represents your "out of pocket" cost - how much actual cash you've put
     * in
     * (minus any cash you've taken out).
     * 
     * Example: Bought $5,000 stocks + deposited $3,000 cash - withdrew $1,000 =
     * $7,000 invested
     * 
     * @return Net amount of money contributed to portfolio
     */
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

    // Add this comment to your current method
    /**
     * MVP LIMITATION: Uses simple average cost method.
     * Does NOT properly implement Canadian Adjusted Cost Base (ACB) rules.
     * Missing: ROC adjustments, superficial loss rules, chronological tracking.
     * 
     * For tax purposes, users should consult professional advice.
     * TODO: Implement proper ACB calculation in future iteration
     * 
     * @throws AccountNotFoundException
     */
    private Money calculateSellGain(Transaction sellTransaction, Portfolio portfolio) throws AccountNotFoundException {
        // Sale proceeds (price * quantity - fees)
        Money saleProceeds = sellTransaction.calculateNetAmount();

        // Get the asset to find its average cost basis
        Account account = portfolio.getAccount(
                portfolio.getAccounts().stream()
                        .filter(ac -> ac.getTransactions().stream()
                                .anyMatch(t -> t.getTransactionId().equals(sellTransaction.getTransactionId())))
                        .map(Account::getAccountId)
                        .findFirst()
                        .orElseThrow()); // need to stream where account has this id
        Asset asset = account.getAsset(sellTransaction.getAssetIdentifier());

        // Calculate cost basis for the quantity sold
        // Using simple average cost method for MVP
        Money averageCostPerUnit = asset.getCostBasis().divide(asset.getQuantity());
        Money costBasisForSale = averageCostPerUnit.multiply(sellTransaction.getQuantity());

        // Realized gain = proceeds - cost basis
        return saleProceeds.subtract(costBasisForSale);
    }

    // TODO: Proper ACB calculation
    /*
     * THIS ON MATTERS CALCULATING CAPTIAL GAINS AND LOSSES ON SALE
     * TAX REPORTING AFTER SELLING ASSETS
     * AND SUPERFICIAL LOSSES
     * 
     * !! USE THE SIMPLIFIED ACB, THE CALCULATESELLGAIN FOR DISPLAYING
     */
    private Money calcualteSellGainWithACB(Transaction sellTransaction, Portfolio portfolio) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private Money calculateFifoGainsForAsset(List<Transaction> transactions, ValidatedCurrency baseCurrency) {
        Queue<CostLot> costBasisQueue = new LinkedList<>();
        Money totalRealizedGains = Money.ZERO(baseCurrency);

        for (Transaction tx : transactions) {
            if (tx.getTransactionType() == TransactionType.BUY) {
                // Calculate TOTAL cost including fees
                Money totalCost = tx.calculateNetAmount();
                if (tx.calculateTotalFees() != null) {
                    totalCost = totalCost.add(tx.calculateTotalFees());
                }

                // Store the ADJUSTED price per unit (includes fee)
                Money adjustedPricePerUnit = totalCost.divide(tx.getQuantity());

                // Add to cost basis queue
                costBasisQueue.add(new CostLot(
                        tx.getQuantity(),
                        adjustedPricePerUnit)
                );
            } 
            else if (tx.getTransactionType() == TransactionType.SELL) {
                Money saleProceeds = tx.calculateNetAmount(); // TODO: Fix the logic here, something is wrong about the calc
                BigDecimal remainingToSell = tx.getQuantity();
                Money totalCostBasis = Money.ZERO(baseCurrency);

                // Match sale against oldest purchases (FIFO)
                while (remainingToSell.compareTo(BigDecimal.ZERO) > 0 && !costBasisQueue.isEmpty()) {
                    CostLot lot = costBasisQueue.peek();
                    BigDecimal soldFromLot = remainingToSell.min(lot.quantity);

                    // Calculate cost basis for this portion
                    Money lotCostBasis = lot.pricePerUnit.multiply(soldFromLot);
                    totalCostBasis = totalCostBasis.add(lotCostBasis);

                    // Update lot
                    lot.quantity = lot.quantity.subtract(soldFromLot);
                    if (lot.quantity.compareTo(BigDecimal.ZERO) == 0) {
                        costBasisQueue.poll(); // Remove empty lot
                    }

                    remainingToSell = remainingToSell.subtract(soldFromLot);
                }

                // Realized gain = proceeds - cost basis
                Money gainFromSale = saleProceeds.subtract(totalCostBasis);
                totalRealizedGains = totalRealizedGains.add(gainFromSale);
            }
        }

        return totalRealizedGains;
    }

    // Helper class to track purchase lots
    private static class CostLot {
        BigDecimal quantity;
        Money pricePerUnit;

        CostLot(BigDecimal quantity, Money pricePerUnit) {
            this.quantity = quantity;
            this.pricePerUnit = pricePerUnit;
        }
    }
}
