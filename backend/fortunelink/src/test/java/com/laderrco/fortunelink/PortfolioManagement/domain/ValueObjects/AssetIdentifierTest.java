package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class AssetIdentifierTest {
    @Test
    void testAssetCommonName() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        AssetIdentifier ai2 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");

        assertEquals(ai1.assetCommonName(), ai2.assetCommonName());
        assertNotEquals(ai1.assetCommonName(), "AAPLLEE");
    }

    // AI assisted
    @Test
    void testCryptoSymbolAccess() {
        // Valid Crypto Asset: tickerSymbol and exchange are empty (which means they're
        // not a traditional asset)
        AssetIdentifier ai1 = new AssetIdentifier("", "", "BTC", "BITCOIN");
        AssetIdentifier ai2 = new AssetIdentifier("", "", "BTC", "BITCOIN");

        assertEquals("BTC", ai1.cryptoSymbol()); // Direct value check
        assertEquals(ai1.cryptoSymbol(), ai2.cryptoSymbol()); // Check equality between two instances
        assertNotEquals("BTCC", ai1.cryptoSymbol()); // Check inequality
    }

    @Test
    void testEquals() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        AssetIdentifier ai2 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        AssetIdentifier ai3 = new AssetIdentifier("APPLeee", "NASDAQ", null, "APPLE");

        assertEquals(ai1, ai2);
        assertNotEquals(ai3, ai2);
    }

    @Test
    void testExchange() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        AssetIdentifier ai2 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        AssetIdentifier ai3 = new AssetIdentifier("APPLeee", "TSX", null, "APPLE");

        assertEquals(ai1.exchange(), ai2.exchange());
        assertNotEquals(ai3.exchange(), ai2.exchange());
    }

    @Test
    void testHashCode() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        AssetIdentifier ai2 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        assertEquals(ai1.hashCode(), ai2.hashCode());

        AssetIdentifier ai3 = new AssetIdentifier("APPLeee", "NASDAQ", null, "APPLE");
        assertFalse(ai1.hashCode() == ai3.hashCode());
    }

    @Test
    void testIsCrypto() {
        AssetIdentifier ai1 = new AssetIdentifier("", "", "BTC", "BITCOIN");
        assertTrue(ai1.isCrypto());
        assertFalse(ai1.isStockOrEtf());
        AssetIdentifier ai2 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        assertFalse(ai2.isCrypto());
    }

    @Test
    void testIsStockOrEtf_forStock() {
        // Create a valid AssetIdentifier for a stock
        AssetIdentifier stock = new AssetIdentifier("AAPL", "NASDAQ", null, "Apple Inc.");
        assertTrue(stock.isStockOrEtf());
        assertFalse(stock.isCrypto()); // Should not be crypto
    }

    @Test
    void testIsStockOrEtf_forEtf() {
        // Create a valid AssetIdentifier for an ETF
        AssetIdentifier etf = new AssetIdentifier("SPY", "NYSEARCA", null, "SPDR S&P 500 ETF Trust");
        assertTrue(etf.isStockOrEtf());
        assertFalse(etf.isCrypto()); // Should not be crypto
    }

    @Test
    void testIsStockOrEtf_forCrypto() {
        // Create a valid AssetIdentifier for a cryptocurrency
        AssetIdentifier crypto = new AssetIdentifier("", "", "BTC", "Bitcoin");
        assertFalse(crypto.isStockOrEtf()); // Should NOT be a stock/ETF
        assertTrue(crypto.isCrypto()); // Should be crypto
    }

    @Test
    void testTickerSymbol() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        assertEquals("APPL", ai1.tickerSymbol());
    }

    @Test
    void testToCanonicalString() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        assertEquals("APPL (NASDAQ)", ai1.toCanonicalString());

        AssetIdentifier ai2 = new AssetIdentifier("", "", "BTC", "BITCOIN");
        assertEquals("BTC", ai2.toCanonicalString());
    }

    @Test
    void testToDisplayName() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        assertEquals("APPLE", ai1.toDisplayName());
        assertNotEquals("APPLE ", ai1.toDisplayName());
    }

    @Test
    void testToString() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        String expectedString = "AssetIdentifier[tickerSymbol=APPL, exchange=NASDAQ, cryptoSymbol=null, assetCommonName=APPLE]";
        assertEquals(expectedString, ai1.toString());
        assertNotEquals("not good", ai1.toString());
    }

    // AI coded
    // Additional tests for your validation rules:

    @Test
    void testTraditionalAssetCreation() {
        assertDoesNotThrow(() -> {
            new AssetIdentifier("AAPL", "NASDAQ", null, "Apple Inc.");
        });
        AssetIdentifier ai = new AssetIdentifier("AAPL", "NASDAQ", null, "Apple Inc.");
        assertEquals("AAPL", ai.tickerSymbol());
        assertEquals("NASDAQ", ai.exchange());
        assertNull(ai.cryptoSymbol());
        assertFalse(ai.isCrypto());
    }

    @Test
    void testCryptoAssetCreation() {
        assertDoesNotThrow(() -> {
            new AssetIdentifier("", "", "ETH", "Ethereum");
        });
        AssetIdentifier ai = new AssetIdentifier("", "", "ETH", "Ethereum");
        assertEquals("", ai.tickerSymbol());
        assertEquals("", ai.exchange());
        assertEquals("ETH", ai.cryptoSymbol());
        assertTrue(ai.isCrypto());
    }

    @Test
    void testConstructorThrowsWhenBothTraditionalAndCryptoProvided() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            new AssetIdentifier("XRP", "NASDAQ", "XRP", "Ripple"); // Ticker/Exchange + Crypto Symbol
        });
        assertEquals("AssetIdentifier cannot represent both a traditional asset and a cryptocurrency.", e.getMessage());
    }

    @Test
    void testConstructorThrowsWhenNeitherTraditionalNorCryptoProvided() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            new AssetIdentifier("", "", null, "Some Asset"); // No ticker/exchange and no crypto symbol
        });
        assertEquals(
                "AssetIdentifier must represent either a traditional asset (ticker/exchange) or a cryptocurrency (crypto symbol).",
                e.getMessage());
    }

    @Test
    void testConstructorThrowsWhenTickerButNoExchange() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            new AssetIdentifier("GOOG", "", null, "Google"); // Ticker but empty exchange
        });
        assertEquals("Exchange cannot be empty if ticker symbol is provided.", e.getMessage());
    }

    @Test
    void testConstructorThrowsWhenExchangeButNoTicker() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            new AssetIdentifier("", "NYSE", null, "NYSE Asset"); // Empty ticker but exchange
        });
        assertEquals("Ticker symbol cannot be empty if exchange is provided.", e.getMessage());
    }

    @Test
    void testCommonNamePreservesCasing() {
        AssetIdentifier ai = new AssetIdentifier("TSLA", "NASDAQ", null, "Tesla, Inc.");
        assertEquals("Tesla, Inc.", ai.assetCommonName());

        AssetIdentifier cryptoAi = new AssetIdentifier("", "", "DOGE", "Dogecoin");
        assertEquals("Dogecoin", cryptoAi.assetCommonName());
    }

    @Test
    void testCryptoSymbolNormalization() {
        AssetIdentifier ai = new AssetIdentifier("", "", " bTc ", "Bitcoin");
        assertEquals("BTC", ai.cryptoSymbol());

        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier("", "", null, "Bitcoin"));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier("", "", "   ", "Bitcoin"));

    }

    @Test
    void testCryptoSymbolNormalization_emptyStringBecomesNull() {
        // Test case where cryptoSymbol is an empty/blank string and should become null
        // This test *requires* a valid traditional asset to avoid the mutual
        // exclusivity error
        // Or, you could construct an invalid one IF YOU EXPECT the constructor to fail
        // for OTHER reasons.
        // But here, we're checking the normalization *behavior* on a valid object.
        AssetIdentifier ai = new AssetIdentifier("AAPL", "NASDAQ", "   ", "Apple Inc.");
        assertNull(ai.cryptoSymbol());
        assertFalse(ai.isCrypto()); // Should not be crypto if cryptoSymbol is null

        // Another valid scenario: traditional asset with null cryptoSymbol
        AssetIdentifier ai2 = new AssetIdentifier("GOOG", "NYSE", null, "Google Inc.");
        assertNull(ai2.cryptoSymbol());
        assertFalse(ai2.isCrypto());
    }
    // This test specifically checks the mutual exclusivity rule for when neither is
    // provided

    // This test also covers the case where the cryptoSymbol input is blank, leading
    // to null,
    // which then triggers the "neither traditional nor crypto" validation
    @Test
    void testConstructorThrowsWhenCryptoSymbolIsBlankAndNoTraditionalAsset() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            new AssetIdentifier("", "", " ", "Some Asset"); // No ticker/exchange AND blank crypto symbol
        });
        assertEquals(
                "AssetIdentifier must represent either a traditional asset (ticker/exchange) or a cryptocurrency (crypto symbol).",
                e.getMessage());
    }

    // Keep your other validation tests separate as well, as you had them:
    @Test
    void testConstructorThrowsWhenBothTraditionalAndCryptoProvided2() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
            new AssetIdentifier("XRP", "NASDAQ", "XRP", "Ripple"); // Ticker/Exchange + Crypto Symbol
        });
        assertEquals("AssetIdentifier cannot represent both a traditional asset and a cryptocurrency.", e.getMessage());
    }

    @Test
    void testIsCryptoMethod() {
        AssetIdentifier cryptoAi = new AssetIdentifier("", "", "ETH", "Ethereum");
        assertTrue(cryptoAi.isCrypto());

        AssetIdentifier traditionalAi = new AssetIdentifier("MSFT", "NASDAQ", null, "Microsoft");
        assertFalse(traditionalAi.isCrypto());
    }

    // You should also test equals() and hashCode() for AssetIdentifier,
    // as records provide them automatically.
    @Test
    void testEqualsAndHashCode() {
        AssetIdentifier ai1 = new AssetIdentifier("AAPL", "NASDAQ", null, "Apple Inc.");
        AssetIdentifier ai2 = new AssetIdentifier("AAPL", "NASDAQ", null, "Apple Inc.");
        AssetIdentifier ai3 = new AssetIdentifier("GOOG", "NASDAQ", null, "Google");
        AssetIdentifier ai4 = new AssetIdentifier("", "", "BTC", "Bitcoin");
        AssetIdentifier ai5 = new AssetIdentifier("", "", "BTC", "Bitcoin");
        AssetIdentifier ai6 = new AssetIdentifier("", "", "ETH", "Ethereum");

        // Traditional asset equality
        assertEquals(ai1, ai2);
        assertEquals(ai1.hashCode(), ai2.hashCode());
        assertNotEquals(ai1, ai3);

        // Crypto asset equality
        assertEquals(ai4, ai5);
        assertEquals(ai4.hashCode(), ai5.hashCode());
        assertNotEquals(ai4, ai6);

        // Different types of assets are not equal
        assertNotEquals(ai1, ai4);
    }
}
