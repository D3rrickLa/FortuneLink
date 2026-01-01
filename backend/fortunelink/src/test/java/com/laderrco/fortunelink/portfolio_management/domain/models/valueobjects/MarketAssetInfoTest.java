package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class MarketAssetInfoTest {
    private MarketAssetInfo testMarketAssetInfo;
    private String symbol;
    private String name;
    private AssetType assetType;
    private String exchange;
    private ValidatedCurrency currency;
    private String sector;
    private Money currentPrice;

    @BeforeEach
    void init() {
        symbol = "AAPL";
        name = "Apple";
        assetType = AssetType.STOCK;
        exchange = "NASDAQ";
        currency = ValidatedCurrency.USD;
        sector = "Technology";
        currentPrice = Money.of(215.55, "USD");
        testMarketAssetInfo = new MarketAssetInfo(symbol, name, assetType, exchange, currency, sector, currentPrice);
    }

    @Test
    void testConstructor() {
        MarketAssetInfo tAssetInfo = new MarketAssetInfo(symbol, name, assetType, exchange, currency, sector, currentPrice);
        assertEquals(tAssetInfo, testMarketAssetInfo);
        assertAll(
            () -> tAssetInfo.getSymbol().equals("AAPL"),
            () -> tAssetInfo.getName().equals("Apple"),
            () -> tAssetInfo.getAssetType().equals(AssetType.STOCK),
            () -> tAssetInfo.getExchange().equals("NASDAQ"),
            () -> tAssetInfo.getCurrency().equals(currency),
            () -> tAssetInfo.getSector().equals("Technology"),
            () -> tAssetInfo.getCurrentPrice().equals(currentPrice)
        );
    }

    @Test 
    void testToIdentifierSucess() {
        MarketIdentifier testIdentifier = new MarketIdentifier(symbol, null, assetType, name, "US$", Map.of("Sector", sector, "Exchange", exchange));
        assertEquals(testIdentifier, testMarketAssetInfo.toIdentifier());
    }
    
    @Test
    @DisplayName("Verify equals and hashCode contracts for MarketAssetInfo")
    void testEqualsAndHashCode() {
        // 1. Create two identical objects
        MarketAssetInfo asset1 = new MarketAssetInfo("AAPL", "Apple Inc.", AssetType.STOCK, "NASDAQ", ValidatedCurrency.of("USD"), "Technology", new Money(new BigDecimal("150.00"), ValidatedCurrency.USD));
        MarketAssetInfo asset2 = new MarketAssetInfo("AAPL", "Apple Inc.", AssetType.STOCK, "NASDAQ", ValidatedCurrency.of("USD"), "Technology", new Money(new BigDecimal("150.00"), ValidatedCurrency.USD));
        
        // 2. Create a different object (changed symbol)
        MarketAssetInfo assetDifferent = new MarketAssetInfo("MSFT", "Apple Inc.", AssetType.STOCK, "NASDAQ", ValidatedCurrency.USD, "Technology",  new Money(new BigDecimal("150.00"), ValidatedCurrency.USD));

        // --- EQUALS CONTRACT TESTS ---
        
        // Reflexive
        assertTrue(asset1.equals(asset1), "Object should be equal to itself");
        
        // Symmetric
        assertTrue(asset1.equals(asset2), "asset1 should equal asset2");
        assertTrue(asset2.equals(asset1), "asset2 should equal asset1");
        
        // Not equal to null
        assertFalse(asset1.equals(null), "Object should not be equal to null");
        
        // Not equal to different type
        assertFalse(asset1.equals(new Object()), "Object should not be equal to a String");
        
        // Not equal to a different object
        assertFalse(asset1.equals(assetDifferent), "Objects with different symbols should not be equal");

        // --- HASHCODE CONTRACT TESTS ---
        
        // Consistency: Equal objects must have same hashcode
        assertEquals(asset1.hashCode(), asset2.hashCode(), "Equal objects must have identical hashCodes");
        
        // Inequality check (though not strictly required by contract, it's good for distribution)
        assertNotEquals(asset1.hashCode(), assetDifferent.hashCode(), "Different objects should ideally have different hashCodes");
    }

    @Test
    @DisplayName("Test that changing any single field breaks equality")
    void testEqualitySensitivity() {
        MarketAssetInfo base = new MarketAssetInfo("AAPL", "Apple", AssetType.STOCK, "NASDAQ", ValidatedCurrency.USD, "Tech", new Money(new BigDecimal("10.00"), ValidatedCurrency.USD));

        // Check every field individually
        assertNotEquals(base, new MarketAssetInfo("OTHER", "Apple", AssetType.STOCK, "NASDAQ", ValidatedCurrency.USD, "Tech", new Money(new BigDecimal("10.00"), ValidatedCurrency.USD)));
        assertNotEquals(base, new MarketAssetInfo("AAPL", "Other", AssetType.STOCK, "NASDAQ", ValidatedCurrency.USD, "Tech", new Money(new BigDecimal("10.00"), ValidatedCurrency.USD)));
        assertNotEquals(base, new MarketAssetInfo("AAPL", "Apple", AssetType.ETF, "NASDAQ", ValidatedCurrency.USD, "Tech", new Money(new BigDecimal("10.00"), ValidatedCurrency.USD)));
        assertNotEquals(base, new MarketAssetInfo("AAPL", "Apple", AssetType.STOCK, "NYSE", ValidatedCurrency.USD, "Tech", new Money(new BigDecimal("10.00"), ValidatedCurrency.USD)));
        assertNotEquals(base, new MarketAssetInfo("AAPL", "Apple", AssetType.STOCK, "NASDAQ", ValidatedCurrency.CAD, "Tech", new Money(new BigDecimal("10.00"), ValidatedCurrency.CAD)));
        assertNotEquals(base, new MarketAssetInfo("AAPL", "Apple", AssetType.STOCK, "NASDAQ", ValidatedCurrency.USD, "Energy", new Money(new BigDecimal("10.00"), ValidatedCurrency.USD)));
        assertNotEquals(base, new MarketAssetInfo("AAPL", "Apple", AssetType.STOCK, "NASDAQ", ValidatedCurrency.USD, "Tech", new Money(new BigDecimal("1.00"), ValidatedCurrency.USD)));
    }
}
