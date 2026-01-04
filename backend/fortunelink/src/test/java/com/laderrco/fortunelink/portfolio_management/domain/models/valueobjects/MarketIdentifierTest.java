package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;

public class MarketIdentifierTest {
    private MarketIdentifier testIdentifier;
    private String Id;
    private AssetType assetType;
    private String name;
    private String uOt;

    @BeforeEach
    void init() {
        Id = "AAPL";
        assetType = AssetType.STOCK;
        name = "Apple";
        uOt = "$US";
        testIdentifier = new MarketIdentifier(Id, null, assetType, name, uOt, null);
    }

    @Test
    void testConstructor_Success() {
        assertEquals("AAPL", testIdentifier.getPrimaryId());;
        assertEquals("Apple", testIdentifier.displayName());
        assertEquals(AssetType.STOCK, testIdentifier.getAssetType());
    }

    @Test
    void testConstructor_ThrowsExceptionWhenIsMarketTradedIsFalse() {
        assertThrows(IllegalArgumentException.class, () ->
            new MarketIdentifier(Id, null, AssetType.COMMODITY, name, uOt, null)
        );
    }

    @Test
    void testConstructor_FailsWhenNullPass() {
        assertThrows(NullPointerException.class, () -> {
            new MarketIdentifier(null, Map.of("null", "null"), assetType, name, Id, Map.of("null", "null"));
            new MarketIdentifier(Id, Map.of("null", "null"), null, name, Id, Map.of("null", "null"));
            new MarketIdentifier(Id, Map.of("null", "null"), assetType, null, Id, Map.of("null", "null"));
            new MarketIdentifier(Id, Map.of("null", "null"), assetType, name, null, Map.of("null", "null"));
        });
    }

    @Test
    void testConstructor_FailsWhenBlankNamePass() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MarketIdentifier(Id, Map.of("null", "null"), assetType, " ", Id, Map.of("null", "null"));
        });
    }
}
