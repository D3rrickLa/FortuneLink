package com.laderrco.fortunelink.PortfolioManagement.domain.Services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.AssetHolding;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Portfolio;
import com.laderrco.fortunelink.PortfolioManagement.domain.Repositories.IPortfolioRepository;
import com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces.IAssetPriceService;
import com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces.IHistoricalAssetPriceService;
import com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces.IInvestmentPerformanceService;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.InvestmentPerformanceSummary;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;

public class InvestmentPerformanceService implements IInvestmentPerformanceService {
    private final IPortfolioRepository portfolioRepository;
    private final IAssetPriceService assetPriceService;
    private final IHistoricalAssetPriceService historicalAssetPriceService;

    public InvestmentPerformanceService(IPortfolioRepository portfolioRepository,
            IAssetPriceService assetPriceService,
            IHistoricalAssetPriceService historicalAssetPriceService) {
        this.portfolioRepository = Objects.requireNonNull(portfolioRepository, "Portfolio Repository cannot be null.");
        this.assetPriceService = Objects.requireNonNull(assetPriceService, "Asset Price Service cannot be null.");
        this.historicalAssetPriceService = Objects.requireNonNull(historicalAssetPriceService,
                "Historical Asset Price Service cannot be null.");
    }

    @Override
    public InvestmentPerformanceSummary calculatePortfolioPerformance(UUID portfolioId) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));

        Set<AssetIdentifier> uniqueAssetIdentifiers = portfolio.getAssets().stream()
                .map(AssetHolding::getAssetIdentifier).collect(Collectors.toSet());
        Map<AssetIdentifier, Money> currentPrices = assetPriceService.getCurrentPrices(uniqueAssetIdentifiers,
                portfolio.getCurrencyPreference());

        Money totalCurrentValue = new Money(BigDecimal.ZERO, portfolio.getCurrencyPreference());
        Money totalCostBasis = new Money(BigDecimal.ZERO, portfolio.getCurrencyPreference());

        for (AssetHolding holding : portfolio.getAssets()) {
            Optional<Money> currentPrice = Optional.ofNullable(currentPrices.get(holding.getAssetIdentifier()));

            if (currentPrice.isPresent()) {
                totalCurrentValue = totalCurrentValue.add(currentPrice.get().multiply(holding.getQuantity()));
            } else {
                // If current price not found, for MVP, we might exclude it from current value
                // or use its cost basis as a fallback if you desire.
                System.err.println(
                        "Warning: Current price not found for asset " + holding.getAssetIdentifier().toCanonicalString()
                                + ". Skipping from current value calculation.");
            }
            totalCostBasis = totalCostBasis.add(holding.getCostBasis());
        }

        // --- Important: Cash Balance ---
        // Your Portfolio aggregate doesn't currently seem to explicitly store a
        // 'cashBalance'.
        // For a full performance calculation, you MUST account for cash.
        // Option 1 (MVP simple): Add a 'cashBalance' field to the Portfolio aggregate.
        // Option 2 (More robust): Implement a way to calculate cash balance from
        // transactions.
        // For this example, I'll assume you add a `portfolio.getCashBalance()` for MVP.
        // If not, initial simple MVP performance might only be for invested assets.
        // Money currentCashBalance = portfolio.getCashBalance(); // Assume this getter
        // exists on Portfolio
        // totalCurrentValue = totalCurrentValue.add(currentCashBalance);

        Money totalGainLoss = totalCurrentValue.subtract(totalCostBasis); // Assuming Money.subtract
        Percentage totalReturnPercentage = Percentage.fromBigDecimal(
                totalCostBasis.amount().compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : totalGainLoss.amount().divide(totalCostBasis.amount(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)));

        // For Annualized Return, you typically need a start date.
        // For MVP, let's assume it's from the portfolio's creation date.
        long daysHeld = ChronoUnit.DAYS.between(portfolio.getCreatedAt(), LocalDate.now());
        Percentage annualizedReturn = calculateAnnualizedReturn(totalReturnPercentage, daysHeld); // Helper method below

        return new InvestmentPerformanceSummary(totalGainLoss, totalReturnPercentage, annualizedReturn);
    }

    // --- Helper Methods (private to the service) ---

    // A simple annualized return calculation (Compound Annual Growth Rate)
    // Only suitable for positive growth; needs more robust handling for losses or
    // short periods.
    private Percentage calculateAnnualizedReturn(Percentage totalReturn, long daysHeld) {
        if (daysHeld <= 0) {
            return new Percentage(BigDecimal.ZERO); // Cannot annualize for zero or negative days
        }
        if (totalReturn.value().compareTo(BigDecimal.valueOf(-100)) <= 0) { // If total loss is 100% or more
            return new Percentage(BigDecimal.valueOf(-100.0)); // Cannot take log of negative or zero base
        }

        BigDecimal totalReturnFactor = BigDecimal.ONE
                .add(totalReturn.value().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        BigDecimal years = BigDecimal.valueOf(daysHeld).divide(BigDecimal.valueOf(365.25), 10, RoundingMode.HALF_UP); // Account
                                                                                                                      // for
                                                                                                                      // leap
                                                                                                                      // years

        // (1 + Total Return)^(1/Years) - 1
        BigDecimal annualizedFactor = BigDecimal.valueOf(Math.pow(totalReturnFactor.doubleValue(),
                BigDecimal.ONE.divide(years, 10, RoundingMode.HALF_UP).doubleValue()));
        BigDecimal annualizedPercentageValue = annualizedFactor.subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100));

        return new Percentage(annualizedPercentageValue.setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    public Percentage calculateTimeWeightedReturn(UUID portfolioId, LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(startDate, "Start date cannot be null.");
        Objects.requireNonNull(endDate, "End date cannot be null.");
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found with ID: " + portfolioId));

        // This is a highly complex calculation. For MVP, consider if you *really* need this.
        // It requires:
        // 1. Determining portfolio value at `startDate`.
        // 2. Identifying all cash inflows/outflows (deposits/withdrawals, dividends) during the period.
        // 3. Determining portfolio value immediately before each cash flow.
        // 4. Determining portfolio value at `endDate`.
        // 5. Fetching historical prices for all assets at each required date.

        // For MVP, you might just have a "Total Gain/Loss" from inception or a simple annualized return.
        // Time-weighted return is often a more advanced feature.
        // This is a placeholder for a much more involved algorithm.

        System.out.println("Warning: calculateTimeWeightedReturn is a complex feature often deferred beyond MVP.");
        // Placeholder return
        return new Percentage(BigDecimal.valueOf(7.5));
    }

}
