package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AllocationItem;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetAllocation;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.services.SimpleExchangeRateService;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class PortfolioTestPart4 {
    private UUID userId;
    private String name;
    private String desc;
    private Money initialPortfolioCashBalance;
    private SimpleExchangeRateService exchangeRateService; // Use SimpleExchangeRateService for controlled rates

    private Currency usd;
    private Currency cad;
    private Portfolio portfolio;
    private UUID portfolioId;
    private Instant testDate;

    // Additional members for assets
    private AssetIdentifier apple;
    private AssetIdentifier microsoft;
    private AssetIdentifier shopify;
    private AssetHolding appleHolding;
    private AssetHolding microsoftHolding;
    private AssetHolding shopifyHolding;
    private Map<AssetIdentifier, MarketPrice> currentPrices;

    @BeforeEach
    void init() {
        usd = Currency.getInstance("USD");
        cad = Currency.getInstance("CAD");
        portfolioId = UUID.randomUUID();
        userId = UUID.randomUUID();
        name = "Test Portfolio";
        desc = "Description of Test Portfolio";
        exchangeRateService = new SimpleExchangeRateService();

        initialPortfolioCashBalance = new Money("12000.00", cad);

        portfolio = new Portfolio(portfolioId, userId, name, desc, initialPortfolioCashBalance, cad, exchangeRateService);
        testDate = Instant.now();

        // --- Setup Assets for Valuation and Allocation Tests ---
        apple = new AssetIdentifier("APPL", AssetType.STOCK, "US0378331005", "APPLE", "NASDAQ", null);
        microsoft = new AssetIdentifier("MSFT", AssetType.STOCK, "US0378331001", "MICROSOFT", "NASDAQ", null);
        shopify = new AssetIdentifier("SHOP", AssetType.STOCK, "US0378331006", "SHOPIFY", "TSX", null); // Assuming SHOP is NYSE or TSX if CAD

        // Apple: 10 shares, bought at $150 USD per share = $1500 USD cost basis
        appleHolding = new AssetHolding(UUID.randomUUID(), portfolio.getPortfolioId(), apple, BigDecimal.TEN, new Money("1500.00", usd), Instant.now());
        // Microsoft: 5 shares, bought at $300 USD per share = $1500 USD cost basis
        microsoftHolding = new AssetHolding(UUID.randomUUID(), portfolio.getPortfolioId(), microsoft, BigDecimal.valueOf(5), new Money("1500.00", usd), Instant.now());
        // Shopify: 20 shares, bought at $50 CAD per share = $1000 CAD cost basis
        shopifyHolding = new AssetHolding(UUID.randomUUID(), portfolio.getPortfolioId(), shopify, BigDecimal.valueOf(20), new Money("1000.00", cad), Instant.now());

        // Add asset holdings to the portfolio (assuming Portfolio has a method for this)
        // You'll need to add a List<AssetHolding> assetHoldings; field to Portfolio
        // and a method like addAssetHolding(AssetHolding holding)
        portfolio.addAssetHolding(appleHolding);
        portfolio.addAssetHolding(microsoftHolding);
        portfolio.addAssetHolding(shopifyHolding);

        // Current Market Prices (as of testDate)
        currentPrices = new HashMap<>();
        currentPrices.put(apple, new MarketPrice(apple, new Money("180.00", usd), testDate, null)); // +$30 per share
        currentPrices.put(microsoft, new MarketPrice(microsoft, new Money("280.00", usd), testDate, null)); // -$20 per share
        currentPrices.put(shopify, new MarketPrice(shopify, new Money("60.00", cad), testDate, null)); // +$10 per share
        // Add currentPrices map to portfolio (if needed) or pass directly to methods
    }

    // Existing reversal tests... (omitted for brevity)

    // ----------------------------------------------------------------------------------------------------
    // New Tests for Valuation, Gains, Allocation, and Interest Accrual
    // ----------------------------------------------------------------------------------------------------

    @Test
    void testCalculateTotalValue() {
        // Expected Calculation:
        // AAPL: 10 shares * $180 USD/share = $1800 USD
        //       $1800 USD * 1.35 CAD/USD = $2430.00 CAD
        // MSFT: 5 shares * $280 USD/share = $1400 USD
        //       $1400 USD * 1.35 CAD/USD = $1890.00 CAD
        // SHOP: 20 shares * $60 CAD/share = $1200 CAD
        // Total Value = $2430.00 CAD + $1890.00 CAD + $1200.00 CAD = $5520.00 CAD

        Money expectedTotalValue = new Money("5584.0000", cad);
        Money actualTotalValue = portfolio.calculateTotalValue(currentPrices);

        assertNotNull(actualTotalValue, "Total value should not be null.");
        assertEquals(cad, actualTotalValue.currency(), "Total value currency should be portfolio preference.");
        assertEquals(expectedTotalValue, actualTotalValue, "Calculated total portfolio value is incorrect.");
    }

    // ----------------------------------------------------------------------------------------------------

    @Test
    void testCalculateUnrealizedGains() {
        // Cost Basis:
        // AAPL: $1500 USD * 1.37 CAD/USD = $2055.00 CAD
        // MSFT: $1500 USD * 1.37 CAD/USD = $2055.00 CAD
        // SHOP: $1000 CAD
        // Total Cost Basis = $2055.00 CAD + $2055.00 CAD + $1000.00 CAD = $5110.00 CAD

        // Total Market Value (from previous test) = $5584.0000 CAD

        // Unrealized Gains = Total Market Value - Total Cost Basis
        // = $5584.00 CAD - $5110.00 CAD = $474.00 CAD

        Money expectedUnrealizedGains = new Money("474.00", cad);
        Money actualUnrealizedGains = portfolio.calculateUnrealizedGains(currentPrices);

        assertNotNull(actualUnrealizedGains, "Unrealized gains should not be null.");
        assertEquals(cad, actualUnrealizedGains.currency(), "Unrealized gains currency should be portfolio preference.");
        assertEquals(expectedUnrealizedGains, actualUnrealizedGains, "Calculated unrealized gains are incorrect.");
    }

    // ----------------------------------------------------------------------------------------------------

    @Test
    void testGetAssetAllocation() {
        // Total Portfolio Value = $5520.00 CAD (from testCalculateTotalValue)
        // Ensure calculateTotalValue works as expected, as getAssetAllocation depends on it.

        // Expected Individual Asset Allocations:
        // AAPL Value: 10 * $180 USD = $1800 USD -> $2466.00 CAD (1800 * 1.37)
        //             % = (2466 / 5584) * 100 = 44.1618... % -> 44.16%
        // MSFT Value: 5 * $280 USD = $1400 USD -> $1918.00 CAD (1400 * 1.37)
        //             % = (1918 / 5584) * 100 = 34.3481... % -> 34.35%
        // SHOP Value: 20 * $60 CAD = $1200 CAD
        //             % = (1200 / 5584) * 100 = 21.4899... % -> 21.49%

        AssetAllocation allocation = portfolio.getAssetAllocation(currentPrices);

        assertNotNull(allocation, "Asset allocation should not be null.");
        assertEquals(portfolio.getCurrencyPreference(), allocation.getBaseCurrency(), "Allocation base currency should match portfolio preference.");
        assertEquals(new Money("5584.00", cad), allocation.getTotalValue(), "Allocation total value should match total portfolio value.");
        assertNotNull(allocation.getCalculatedAt(), "CalculatedAt timestamp should be set.");

        // --- Verify allocationsBySymbol ---
        Map<String, AllocationItem> allocationsBySymbol = allocation.getAllocationsBySymbol();
        assertEquals(3, allocationsBySymbol.size(), "Should have 3 symbol allocations.");

        // Check AAPL
        AllocationItem aaplItem = allocationsBySymbol.get("APPL");
        assertNotNull(aaplItem, "APPL allocation should exist.");
        assertEquals(new Money("2466.0000", cad), aaplItem.value(), "AAPL allocated value incorrect.");
        assertEquals(new Percentage(new BigDecimal("44.1619")), aaplItem.percentage(), "AAPL percentage incorrect.");
        assertEquals(apple, aaplItem.assetIdentifier(), "AAPL AllocationItem should retain correct AssetIdentifier.");

        // Check MSFT
        AllocationItem msftItem = allocationsBySymbol.get("MSFT");
        assertNotNull(msftItem, "MSFT allocation should exist.");
        assertEquals(new Money("1918.0000", cad), msftItem.value(), "MSFT allocated value incorrect.");
        assertEquals(new Percentage(new BigDecimal("34.348100")), msftItem.percentage(), "MSFT percentage incorrect.");
        assertEquals(microsoft, msftItem.assetIdentifier(), "MSFT AllocationItem should retain correct AssetIdentifier.");

        // Check SHOP
        AllocationItem shopItem = allocationsBySymbol.get("SHOP");
        assertNotNull(shopItem, "SHOP allocation should exist.");
        assertEquals(new Money("1200.00", cad), shopItem.value(), "SHOP allocated value incorrect.");
        assertEquals(new Percentage(new BigDecimal("21.490000")), shopItem.percentage(), "SHOP percentage incorrect.");
        assertEquals(shopify, shopItem.assetIdentifier(), "SHOP AllocationItem should retain correct AssetIdentifier.");

        // Verify total percentage by symbol (sum of rounded percentages might not be exactly 100)
        BigDecimal sumSymbolPercentages = allocationsBySymbol.values().stream()
            .map(item -> item.percentage().percentageValue())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertTrue(sumSymbolPercentages.compareTo(new BigDecimal("99.99")) >= 0 && sumSymbolPercentages.compareTo(new BigDecimal("100.01")) <= 0,
            "Sum of symbol percentages should be approximately 100%. Found: " + sumSymbolPercentages);


        // --- Verify allocationsByType ---
        Map<AssetType, AllocationItem> allocationsByType = allocation.getAllocationsByType();
        assertEquals(1, allocationsByType.size(), "Should have 1 asset type allocation (all STOCK).");

        // Check STOCK type
        AllocationItem stockTypeItem = allocationsByType.get(AssetType.STOCK);
        assertNotNull(stockTypeItem, "STOCK type allocation should exist.");
        // Value: 2430.00 (AAPL) + 1890.00 (MSFT) + 1200.00 (SHOP) = 5520.00 CAD
        assertEquals(new Money("5584.0000", cad), stockTypeItem.value(), "STOCK type allocated value incorrect.");
        // Percentage: 44.02 + 34.24 + 21.74 = 100.00%
        assertEquals(new Percentage(new BigDecimal("100.00")), stockTypeItem.percentage(), "STOCK type percentage incorrect.");
        // The assetIdentifier in the type aggregation should be the last one added or representative, but not strictly tested here
        // as its purpose is aggregation. Can test its assetType if needed (e.g., stockTypeItem.assetIdentifier().assetType() == AssetType.STOCK)


        // --- Test getPercentageBySymbol ---
        assertEquals(new Percentage(new BigDecimal("44.1619")), allocation.getPercentageBySymbol("APPL"), "getPercentageBySymbol for AAPL incorrect.");
        assertEquals(new Percentage(BigDecimal.ZERO), allocation.getPercentageBySymbol("NONEXISTENT"), "getPercentageBySymbol for non-existent symbol should be zero.");

        // --- Test getPercentageByType ---
        assertEquals(new Percentage(new BigDecimal("100.00")), allocation.getPercentageByType(AssetType.STOCK), "getPercentageByType for STOCK incorrect.");
        assertEquals(new Percentage(BigDecimal.ZERO), allocation.getPercentageByType(AssetType.BOND), "getPercentageByType for non-existent type should be zero.");

        // --- Test isDiversified ---
        // Max allocation 50% -> not diversified (AAPL is 44.02%, MSFT 34.24%, SHOP 21.74%)
        assertTrue(allocation.isDiversified(new Percentage(new BigDecimal("50.00"))), "Portfolio should be diversified with max 50% (none exceed).");
        assertFalse(allocation.isDiversified(new Percentage(new BigDecimal("40.00"))), "Portfolio should NOT be diversified with max 40% (AAPL exceeds).");

        // --- Test getTopAllocations ---
        List<AllocationItem> top2 = allocation.getTopAllocations(2);
        assertEquals(2, top2.size(), "Should return top 2 allocations.");
        assertEquals("APPL", top2.get(0).assetIdentifier().symbol(), "Top 1 should be AAPL.");
        assertEquals("MSFT", top2.get(1).assetIdentifier().symbol(), "Top 2 should be MSFT.");

        List<AllocationItem> top5 = allocation.getTopAllocations(5); // Request more than available
        assertEquals(3, top5.size(), "Should return all available if n is greater than total.");
        assertEquals("APPL", top5.get(0).assetIdentifier().symbol());
    }

    // ----------------------------------------------------------------------------------------------------

    @Test
    void testAccrueInterestLiabilities() {
        // --- Setup a Liability for Interest Accrual ---
        Money loanAmount = new Money("1000.00", cad);
        Percentage annualInterestRate = new Percentage(new BigDecimal("0.05")); // 5% annual interest
        Instant incurrenceDate = testDate.minus(30, ChronoUnit.DAYS); // Loan incurred 30 days ago
        Instant lastAccrualDate = incurrenceDate; // Assume last accrual was on incurrence date

        // Manually create and add a liability for testing accrual,
        // as recordNewLiability might add a transaction and affect cash.
        // For a clean test of just accrual, create directly.
        Liability testLiability = new Liability(
                UUID.randomUUID(),
                portfolioId,
                "loan name",
                "loan desc",
                loanAmount, // originalLoanAmountInLiabilityCurrency
                annualInterestRate,
                testDate.plus(365, ChronoUnit.DAYS), // Maturity in 1 year,
                Instant.now()
        );
        testLiability.setLastAccrualDate(lastAccrualDate);
        portfolio.addLiability(testLiability);

        assertEquals(new Money("1000.00", cad), testLiability.getCurrentBalance(), "Initial liability balance should be loan amount.");
        assertEquals(0, portfolio.getTransactions().size(), "No transactions before accrual.");

        Instant accrualRunDate = testDate;


        // Calculate expected interest: Principal * (Annual Rate / 365) * Days
        // 1000 * (0.05 / 365) * 30 days = 1000 * 0.0001369863 * 30 = 4.109589 -> 4.11 CAD (rounded)
        BigDecimal expectedInterestAmountBd1 = new BigDecimal("1000.00")
            .multiply(new BigDecimal("0.05"))
            .divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP) // More precision for intermediate
            .multiply(new BigDecimal("30"))
            .setScale(2, RoundingMode.HALF_UP); // Round to 2 decimal places for final money amount

        Money preciseAccruedInterest1 = new Money(expectedInterestAmountBd1, cad); // Approx 4.1095890411 CAD

        portfolio.accruelInterestLiabilities(accrualRunDate);

        // Expected liability balance: 1000.00 + 4.11 = 1004.11 CAD
        Money expectedLiabilityBalance = new Money("1004.11", cad);
        assertEquals(expectedLiabilityBalance, testLiability.getCurrentBalance(), "Liability balance should increase by accrued interest.");

        // Verify a new transaction was recorded for interest accrual
        assertEquals(1, portfolio.getTransactions().size(), "Should have 1 transaction for interest accrual.");
        Transaction accrualTx = portfolio.getTransactions().get(0);
        assertEquals(TransactionType.INTEREST, accrualTx.getTransactionType(), "Transaction type should be INTEREST_ACCRUAL.");
        assertEquals(preciseAccruedInterest1, accrualTx.getTotalTransactionAmount(), "Accrual transaction amount should be the accrued interest.");
        assertEquals(testLiability.getLiabilityId(), accrualTx.getParentTransactionId(), "Accrual transaction should link to the liability ID."); // Assuming originalTransactionId is used for linking
        assertEquals(accrualRunDate, testLiability.getLastInterestAccrualDate(), "Last accrual date on liability should be updated.");

        // Run accrual again for no additional days
        portfolio.accruelInterestLiabilities(accrualRunDate);
        assertEquals(expectedLiabilityBalance, testLiability.getCurrentBalance(), "Liability balance should not change if no days passed.");
        assertEquals(1, portfolio.getTransactions().size(), "No new transactions should be created if no days passed.");

        // Accrue for more days
        Instant nextAccrualRunDate = accrualRunDate.plus(15, ChronoUnit.DAYS);
        // Interest for 15 days = 1004.11 * (0.05 / 365) * 15 = 2.06 CAD (approx)
        BigDecimal expectedInterestAmountBd2 = new BigDecimal("1004.11")
            .multiply(new BigDecimal("0.05"))
            .divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("15"))
            .setScale(2, RoundingMode.HALF_UP);
        Money expectedAccruedInterest2 = new Money(expectedInterestAmountBd2, cad);
        
        portfolio.accruelInterestLiabilities(nextAccrualRunDate);
        assertEquals(expectedLiabilityBalance.add(expectedAccruedInterest2), testLiability.getCurrentBalance().roundToTwoDecimalPlaces(), "Liability balance should increase by new accrued interest.");
        assertEquals(2, portfolio.getTransactions().size(), "Should have 2 transactions after second accrual.");
        assertEquals(nextAccrualRunDate, testLiability.getLastInterestAccrualDate(), "Last accrual date should be updated to next run date.");
    }
}
