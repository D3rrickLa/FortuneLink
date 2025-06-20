package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class NetWorthSummaryTest {
    @Test
    void testEquals() {
        NetWorthSummary nws1 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));
        NetWorthSummary nws2 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));

        assertEquals(nws1, nws2);
    }

    // AI coded
    @Test
    void testEqualsTwo() {
        // Calculate the expected netWorthValue correctly
        Money assets = new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$"));
        Money liabilities = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Money expectedNetWorth = assets.subtract(liabilities); // This will be Money(900, "USD")

        NetWorthSummary nws1 = new NetWorthSummary(
                assets,
                liabilities,
                expectedNetWorth // Use the correctly calculated value here
        );
        NetWorthSummary nws2 = new NetWorthSummary(
                assets, // Re-using for brevity, but could create new instances
                liabilities,
                expectedNetWorth);

        assertEquals(nws1, nws2);

        // --- Additional comprehensive equals tests ---

        // Test with different assets, but resulting in same netWorth (if logic allowed
        // this, which it usually wouldn't for equals)
        // This is more for demonstrating how records' equals work.
        // Money assetsDifferent = new Money(new BigDecimal(1050), new
        // PortfolioCurrency("USD", "$"));
        // Money liabilitiesDifferent = new Money(new BigDecimal(150), new
        // PortfolioCurrency("USD", "$"));
        // NetWorthSummary nws3 = new NetWorthSummary(assetsDifferent,
        // liabilitiesDifferent, expectedNetWorth);
        // assertFalse(nws1.equals(nws3)); // Should be false because assets/liabilities
        // are different

        // Test with different currency
        NetWorthSummary nwsDifferentCurrency = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("EUR", "€")),
                new Money(new BigDecimal(100), new PortfolioCurrency("EUR", "€")),
                new Money(new BigDecimal(900), new PortfolioCurrency("EUR", "€")) // Correct net worth for EUR
        );
        assertEquals(false, nws1.equals(nwsDifferentCurrency));

        // Test self-equality
        assertEquals(nws1, nws1);

        // Test null comparison
        assertEquals(false, nws1.equals(null));

        // Test with a different type
        assertEquals(false, nws1.equals("a string")); // Should return false
    }

    // You should also have test for the validation logic itself!
    @Test
    void testNetWorthSummaryValidation() {
        Money assets = new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$"));
        Money liabilities = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Money incorrectNetWorth = new Money(new BigDecimal(1100), new PortfolioCurrency("USD", "$"));

        // Test that it throws an exception when net worth is inconsistent
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new NetWorthSummary(assets, liabilities, incorrectNetWorth);
        });
        assertEquals("Net worth value does not equal assets minus liabilities.", thrown.getMessage());
    }

    // ----
    @Test
    void testHashCode() {
        NetWorthSummary nws1 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));
        NetWorthSummary nws2 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));

        assertEquals(nws1.hashCode(), nws2.hashCode());
    }

    @Test
    void testNetWorthValue() {
        NetWorthSummary nws1 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));

        assertEquals(new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")), nws1.netWorthValue());
    }

    @Test
    void testToString() {
        NetWorthSummary nws1 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));
        String expectedToString = "NetWorthSummary[totalAssets=Money[amount=1000.0000, currencyCode=PortfolioCurrency[code=USD, symbol=$]], totalLiabilities=Money[amount=100.0000, currencyCode=PortfolioCurrency[code=USD, symbol=$]], netWorthValue=Money[amount=900.0000, currencyCode=PortfolioCurrency[code=USD, symbol=$]]]";
        assertEquals(expectedToString, nws1.toString());
    }

    @Test
    void testTotalAssets() {
        NetWorthSummary nws1 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));
        NetWorthSummary nws2 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));

        assertEquals(new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")), nws2.totalAssets());
        assertEquals(nws1.totalAssets(), nws2.totalAssets());
    }

    @Test
    void testTotalLiabilities() {
        NetWorthSummary nws1 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));
        NetWorthSummary nws2 = new NetWorthSummary(
                new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")),
                new Money(new BigDecimal(900), new PortfolioCurrency("USD", "$")));

        assertNotEquals(new Money(new BigDecimal(100), new PortfolioCurrency("EUR", "$")), nws2.totalLiabilities());
        assertEquals(new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$")), nws2.totalLiabilities());
        assertEquals(nws1.totalLiabilities(), nws2.totalLiabilities());
    }
}
