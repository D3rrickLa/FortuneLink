package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class AssetIdentifierTest {

    @Test
    void testConstructor() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        AssetIdentifier ai2 = new AssetIdentifier(null, "COINGEECKO", "BTC", "BITCOIN");

        // testing if both ticker and crypto symbol are null;
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(null, "NASDAQ", null, "APPLE"));

        // testing if both ticker and crypto symbol are present, should throw error
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier("APPL", "NASDAQ", "APPLE", "APPLE"));

        assertNotEquals(ai1, ai2);
    }

    @Test
    void testAssetCommonName() {

    }

    @Test
    void testCryptoSymbol() {

    }

    @Test
    void testEquals() {

    }

    @Test
    void testExchange() {

    }

    @Test
    void testHashCode() {

    }

    @Test
    void testIsCrypto() {

    }

    @Test
    void testIsStockOrETF() {

    }

    @Test
    void testTickerSymbol() {

    }

    @Test
    void testToString() {

    }
}
