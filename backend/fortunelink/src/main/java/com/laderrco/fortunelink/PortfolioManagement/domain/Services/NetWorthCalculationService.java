package com.laderrco.fortunelink.PortfolioManagement.domain.Services;

import java.math.BigDecimal;
import java.time.Period;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.AssetHolding;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Liability;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Portfolio;
import com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces.IAssetPriceService;
import com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces.INetWorthCalculationService;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.NetWorthSummary;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;

// AI coded for the most part ☠️☠️☠️
public class NetWorthCalculationService implements INetWorthCalculationService {

    private final IAssetPriceService assetPriceService;

    public NetWorthCalculationService(IAssetPriceService assetPriceService) {
        this.assetPriceService = Objects.requireNonNull(assetPriceService, "AssetPrice cannot be null.");
    }

    @Override
    public NetWorthSummary calculateNetWorth(Portfolio portfolio) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null.");

        Set<AssetIdentifier> uniqueAssetIdentifiers = portfolio.getAssets().stream()
                .map(AssetHolding::getAssetIdentifier)
                .collect(Collectors.toSet());

        Map<AssetIdentifier, Money> currentPrices = assetPriceService.getCurrentPrices(uniqueAssetIdentifiers,
                portfolio.getCurrencyPreference());

        Money totalAssetsValue = calculateTotalAssetsValue(portfolio, currentPrices);
        Money totalLiabilitiesValue = calculateTotalLiabilitiesValue(portfolio);
        Money netWorth = totalAssetsValue.subtract(totalLiabilitiesValue);

        return new NetWorthSummary(totalAssetsValue, totalLiabilitiesValue, netWorth);

    }

    @Override
    public NetWorthSummary calculateProjectedNetWorth(Portfolio portfolio, int monthsAhead) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null.");
        if (monthsAhead < 0) {
            throw new IllegalArgumentException("Months ahead must be non-negative.");
        }
        // --- MVP Simplification ---
        // For MVP, projecting net worth is complex due to needing assumptions about
        // future:
        // 1. Future contributions/withdrawals
        // 2. Future asset returns
        // 3. Future liability payments/interest accruals

        // A basic MVP projection might assume:
        // - No future contributions/withdrawals (or a fixed monthly amount)
        // - A fixed average annual growth rate for assets
        // - Liabilities continue to accrue interest/be paid down at current rates

        // This method will rely heavily on assumptions and potentially the
        // InvestmentPerformanceService
        // and GoalProjectionService (which is later for you).

        NetWorthSummary currentNetWorthSummary = calculateNetWorth(portfolio);
        Money currentNetWorth = currentNetWorthSummary.netWorthValue();

        // Assumption 1: Fixed average monthly growth rate (e.g., based on historical
        // average or a user input expectation)
        // For a true MVP, you might hardcode this or fetch a very simple "expected
        // return" from somewhere.
        // This is a placeholder for a more sophisticated financial model.
        BigDecimal assumedMonthlyGrowthRate = BigDecimal.valueOf(0.005); // Example: 0.5% monthly growth
        // A more realistic approach would use a weighted average return from the
        // InvestmentPerformanceService
        // or a Monte Carlo simulation (definitely not MVP).

        // For liabilities, a simple model might assume they decrease based on payments,
        // but for MVP, this can be complex to model automatically.
        // Let's simplify: project current net worth forward with a growth rate,
        // ignoring complex liability changes for now.
        // If Liabilities *do* change over time, the NetWorthSummary would need more
        // detail or a more complex model.

        Money projectedNetWorth = currentNetWorth;
        for (int i = 0; i < monthsAhead; i++) {
            // projectedNetWorth =
            // projectedNetWorth.add(projectedNetWorth.multiply(assumedMonthlyGrowthRate));
            // // Compound growth
            // Or if you have a way to model future contributions:
            // projectedNetWorth = projectedNetWorth.add(assumedMonthlyContribution);
            // This is where the actual projection logic will get much more complex.
            // For MVP, it might be as simple as compounding based on an assumed growth
            // rate.
            projectedNetWorth = projectedNetWorth.multiply(BigDecimal.ONE.add(assumedMonthlyGrowthRate)); // Compounding
        }

        // The NetWorthSummary might need to be extended to hold projected data points
        // For MVP, a single projected value is often enough.
        return new NetWorthSummary(projectedNetWorth, null, null); // Placeholder timestamp

    }

    @Override
    public Percentage calculateNetWorthGrowthRate(Portfolio portfolio, Period period) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null.");
        Objects.requireNonNull(period, "Period cannot be null.");

        // --- MVP Simplification ---
        // Calculating historical growth rate requires historical net worth snapshots.
        // If your MVP does NOT store historical snapshots of net worth, this method
        // will be difficult to implement accurately.
        // You would need:
        // 1. Historical asset prices for the start and end of the period.
        // 2. A way to determine cash balance at the start and end of the period (from
        // transactions).
        // 3. Liability balances at the start and end of the period.

        // For MVP, you might:
        // a) Return a placeholder value.
        // b) Only support it if you implement daily net worth snapshots (which is a
        // significant feature).
        // c) Limit it to simple "total gain since creation" if you don't have
        // historical data.

        // If you only have current state, you could calculate:
        // (Current Net Worth - Initial Investment) / Initial Investment * 100
        // This is a simplified "total return" rather than a true "growth rate over a
        // period".

        // Let's assume for a robust MVP, you will get current net worth and need a
        // 'starting' net worth.
        // How to get historical net worth? This is where an
        // `IPortfolioSnapshotRepository` or similar
        // would come in, which likely isn't MVP.

        // For a very basic implementation, if 'period' is not too far in the past,
        // and you can *reconstruct* the portfolio state at the start of the period
        // using historical transactions and asset prices, you could. This is
        // non-trivial.

        // For MVP, a practical approach might be to calculate the growth rate
        // from the portfolio's creation date to now, if you are not storing snapshots.
        // Or, defer this method until historical data capture is in place.

        // Placeholder for a more complex calculation that would involve:
        // Money netWorthStart = calculateNetWorthAtDate(portfolio,
        // period.getStartDate());
        // Money netWorthEnd = calculateNetWorthAtDate(portfolio, period.getEndDate());
        // return new
        // Percentage((netWorthEnd.subtract(netWorthStart)).divide(netWorthStart).multiply(BigDecimal.valueOf(100)));

        // --- Very Simplified MVP placeholder (e.g., always returns 5% for now) ---
        System.out.println("Warning: calculateNetWorthGrowthRate is a placeholder for MVP without historical data.");
        return new Percentage(BigDecimal.valueOf(5.0)); // Placeholder
    }

    // AI coded
    // --- Helper Methods (private to the service) ---

    private Money calculateTotalAssetsValue(Portfolio portfolio, Map<AssetIdentifier, Money> currentPrices) {
        Money totalValue = new Money(BigDecimal.ZERO, portfolio.getCurrencyPreference()); // Assuming Money has a
                                                                                          // constructor for zero

        // 1. Sum up value of asset holdings
        for (AssetHolding holding : portfolio.getAssets()) {
            Optional<Money> price = Optional.ofNullable(currentPrices.get(holding.getAssetIdentifier()));
            if (price.isPresent()) {
                Money holdingValue = price.get().multiply(holding.getQuantity());
                totalValue = totalValue.add(holdingValue); // Assuming Money.add exists
            } else {
                // Handle cases where price isn't available (e.g., log, throw error, skip)
                // For MVP, you might default to cost basis or skip.
                System.err.println("Warning: Current price not found for asset "
                        + holding.getAssetIdentifier().toCanonicalString());
                // For a more robust system, you might want to consider the last known price or
                // its cost basis here.
                // For MVP, if a price isn't available, its market value might not be included.
            }
        }

        // 2. Include any direct cash balance in the portfolio (if modeled this way)
        // If Portfolio has a getCashBalance() method:
        // totalValue = totalValue.add(portfolio.getCashBalance());

        return totalValue;
    }

    private Money calculateTotalLiabilitiesValue(Portfolio portfolio) {
        Money totalValue = new Money(BigDecimal.ZERO, portfolio.getCurrencyPreference()); // Assuming Money has a
                                                                                          // constructor for zero

        for (Liability liability : portfolio.getLiabilities()) {
            totalValue = totalValue.add(liability.getCurrentBalance()); // Assuming Money.add and
                                                                        // Liability.getCurrentBalance exist
        }
        return totalValue;
    }

}
