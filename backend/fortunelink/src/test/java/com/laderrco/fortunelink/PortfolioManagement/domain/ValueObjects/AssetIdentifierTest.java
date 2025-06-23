package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssetIdentifierTest {
    private AssetIdentifier asset;
    private AssetIdentifier asset2;
    
    @BeforeEach
    void init(){
        asset = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        asset2 = new AssetIdentifier(null, "COINGEECKO", "BTC", "BITCOIN");

    }

    @Test
    void testConstructor() {
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        AssetIdentifier ai2 = new AssetIdentifier(null, "COINGEECKO", "BTC", "BITCOIN");
        
        // testing if both ticker and crypto symbol are null;
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(null, "NASDAQ", null, "APPLE"));
        
        // testing when ticker symbol is given blank string
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier("", "NASDAQ", null, "APPLE"));
        
        // testing when crypto symbol is given blank string
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(null, "NASDAQ", "", "APPLE"));
        
        // testing if both ticker and crypto symbol are present, should throw error
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier("APPL", "NASDAQ", "APPLE", "APPLE"));
        
        assertNotEquals(ai1, ai2);
    }
    
    @Test
    void testAssetCommonName() {
        assertTrue(asset.assetCommonName().equals("APPLE"));
    }
    
    @Test
    void testCryptoSymbol() {
        assertEquals("BTC", asset2.cryptoSymbol());
    }
    
    @Test
    void testEquals() {
        assertNotEquals(asset2, asset);
        AssetIdentifier ai1 = new AssetIdentifier("APPL", "NASDAQ", null, "APPLE");
        assertEquals(ai1, asset);
    }

    @Test
    void testExchange() {
        assertEquals("NASDAQ", asset.exchange());
        assertNotEquals("NASDAQ  ", asset.exchange());
    }

    @Test
    void testHashCode() {
        assertFalse(asset.hashCode() == asset2.hashCode());
    }

    @Test
    void testIsCrypto() {
        assertFalse(asset.isCrypto());
        assertTrue(asset2.isCrypto());
    }
    
    @Test
    void testIsStockOrETF() {
        assertFalse(asset2.isStockOrETF());
        assertTrue(asset.isStockOrETF());

    }

    @Test
    void testTickerSymbol() {
        assertEquals("APPL", asset.tickerSymbol());
        assertNotEquals(" APPL", asset.tickerSymbol());
        assertNotEquals(" ", asset.tickerSymbol());
    }

    @Test
    void testToString() {
        assertNotNull(asset.toString());
    }
}
