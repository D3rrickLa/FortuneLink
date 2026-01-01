package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

// ANSWERS THE QUESTION: HOW IS IT PERFORMING (COMPLEX, USES VALUATION + HISTORY)
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
    public Percentage calculateTotalReturn(Portfolio portfolio, MarketDataService marketDataService, ExchangeRateService exchangeRateService) {
        Money currentValue = calculateCurrentValue(portfolio, marketDataService, exchangeRateService);
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
        ValidatedCurrency portfolioBaseCurrency = portfolio.getPortfolioCurrencyPreference();

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

    public Money calculateRealizedGainsCAD_ACB(Portfolio portfolio, ExchangeRateService exchangeRateService, List<Transaction> transactions) {
        if (portfolio == null) {
            return Money.ZERO("CAD"); 
        }
        
        return calculateSellGainWithACB(portfolio, exchangeRateService, transactions);
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
        ValidatedCurrency baseCurrency = portfolio.getPortfolioCurrencyPreference();

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

    private Money calculateCurrentValue(Portfolio portfolio, MarketDataService marketDataService, ExchangeRateService exchangeRateService) {
        // we are going to use the PortfolioEvaluationService.java for calculation
        // rather than a direct implementation
        PortfolioValuationService portfolioValuationService = new PortfolioValuationService(marketDataService, exchangeRateService);
        return portfolioValuationService.calculateTotalValue(portfolio, Instant.now());
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
        ValidatedCurrency baseCurrency = portfolio.getPortfolioCurrencyPreference();

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

    

    private Money calculateSellGainWithACB(Portfolio portfolio, ExchangeRateService exchangeRateService, List<Transaction> transactions) {
        BigDecimal runningTotalShares = BigDecimal.ZERO;
        BigDecimal runningTotalCostCAD = BigDecimal.ZERO;
        BigDecimal totalRealizedGainCAD = BigDecimal.ZERO;

        // Transactions must be sorted by date for ACB to be accurate
        List<Transaction> sortedTransactions = transactions.stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .toList();

        for (Transaction tx : sortedTransactions) {
            BigDecimal txShares = tx.getQuantity();
            // Ensure we have the CAD value at the time of transaction
            BigDecimal txPriceCAD = exchangeRateService.convert(tx.getPricePerUnit(), portfolio.getPortfolioCurrencyPreference()).amount(); 

            switch (tx.getTransactionType()) {
                case BUY, TRANSFER_IN -> {
                    // TRANSFER_IN usually arrives with an existing cost basis
                    runningTotalShares = runningTotalShares.add(txShares);
                    runningTotalCostCAD = runningTotalCostCAD.add(txShares.multiply(txPriceCAD));
                }

                case SELL, TRANSFER_OUT -> {
                    // 1. Calculate ACB safely
                    BigDecimal acbPerShare = (runningTotalShares.compareTo(BigDecimal.ZERO) > 0) 
                        ? runningTotalCostCAD.divide(runningTotalShares, 10, RoundingMode.HALF_UP) 
                        : BigDecimal.ZERO;

                    if (tx.getTransactionType() == TransactionType.SELL) {
                        // 2. ONLY calculate a gain if we actually owned shares to sell
                        // If runningTotalShares is 0, the gain should be 0 (or handled as a short)
                        BigDecimal gain = BigDecimal.ZERO;
                        if (runningTotalShares.compareTo(BigDecimal.ZERO) > 0) {
                            gain = txPriceCAD.subtract(acbPerShare).multiply(txShares);
                        }
                        totalRealizedGainCAD = totalRealizedGainCAD.add(gain);
                    }

                    // 3. Update the pool (this may push shares into negative if it's a short)
                    runningTotalShares = runningTotalShares.subtract(txShares);
                    runningTotalCostCAD = runningTotalCostCAD.subtract(acbPerShare.multiply(txShares));
                }
                
                case REINVESTED_CAPITAL_GAIN -> { // Phantom Distribution
                        // 1. Add the value to the total cost base
                        // 2. DO NOT change the runningTotalShares (they were consolidated)
                        runningTotalCostCAD = runningTotalCostCAD.add(txPriceCAD);
                    }

                case RETURN_OF_CAPITAL -> {
                    // 1. Subtract value from total cost base
                    runningTotalCostCAD = runningTotalCostCAD.subtract(txPriceCAD);

                    // 2. CRA Rule: If ACB drops below zero, the negative amount is a capital gain
                    if (runningTotalCostCAD.compareTo(BigDecimal.ZERO) < 0) {
                        totalRealizedGainCAD = totalRealizedGainCAD.add(runningTotalCostCAD.abs());
                        runningTotalCostCAD = BigDecimal.ZERO;
                    }
                }
                case DIVIDEND -> {
                    // IMPORTANT: Only process this if it's a DRIP (Reinvested).
                    // If it's just cash hitting the account, ignore it for ACB.
                    if (txShares.compareTo(BigDecimal.ZERO) > 0 && tx.isDrip()) {
                        runningTotalShares = runningTotalShares.add(txShares);
                        runningTotalCostCAD = runningTotalCostCAD.add(txPriceCAD); 
                    }
                }
                default -> {
                    // DEPOSIT, WITHDRAWAL, INTEREST, FEE, OTHER
                    // These typically affect cash balance, not the asset's ACB.
                    // Ignoring them here will turn your coverage GREEN.
                }
                
            }
        }
        return new Money(totalRealizedGainCAD, portfolio.getPortfolioCurrencyPreference());
    }

    private Money calculateFifoGainsForAsset(List<Transaction> transactions, ValidatedCurrency baseCurrency) {
        Queue<CostLot> costBasisQueue = new LinkedList<>();
        Money totalRealizedGains = Money.ZERO(baseCurrency);

        for (Transaction tx : transactions) {
            if (tx.getTransactionType() == TransactionType.BUY) {
                Money totalCost = tx.calculateTotalCost(); // Includes fee
                Money adjustedPricePerUnit = totalCost.divide(tx.getQuantity());
                costBasisQueue.add(new CostLot(tx.getQuantity(), adjustedPricePerUnit));
            } 
            else if (tx.getTransactionType() == TransactionType.SELL) {
                Money saleProceeds = tx.calculateTotalCost();
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
