package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.AssetType;

public class AssetIdentifierTest {
    private AssetIdentifier asset;
    private AssetIdentifier asset2;
    
    @BeforeEach
    void init(){
        asset = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        asset2 = new AssetIdentifier(AssetType.CRYPTO, "BTC", "BITCOIN", "COINGECKO");
        asset2 = new AssetIdentifier(AssetType.BOND, "BTC", "BITCOIN", "COINGECKO");
        asset2 = new AssetIdentifier(AssetType.COMMODITY, "BTC", "BITCOIN", "COINGECKO");
        asset2 = new AssetIdentifier(AssetType.FOREX_PAIR, "BTC", "BITCOIN", "COINGECKO");
        asset2 = new AssetIdentifier(AssetType.CRYPTO, "BTC", "BITCOIN", "COINGECKO");

    }

    @Test
    void testConstructor() {
        AssetIdentifier ai1 = new AssetIdentifier(AssetType.ETF, "APPL", "APPLE", "NASDAQ");
        AssetIdentifier ai2 = new AssetIdentifier(AssetType.CRYPTO, "BTC", "BITCOIN", null);
        
        // Stocks / ETFs
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.ETF, "APPL", "\n\n\n\r", "NASDAQ"));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.ETF, "", "APPLE", "NASDAQ"));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.ETF, "APPL", "APPLE", null));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.ETF, "APPL", "APPLE", ""));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.ETF, "APPL", "APPLE", "   \n"));

        // BONDs
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.BOND, "", "APPLE", null));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.BOND, "\r\r\r", "APPLE", null));
        
        // CYRPTO
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.CRYPTO, "", "APPLE", null));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.CRYPTO, "\r\r\r", "APPLE", null));
        
        // COMMODITY
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.COMMODITY, "", "APPLE", null));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.COMMODITY, "\r\r\r", "APPLE", null));
        
        // FOREX
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.FOREX_PAIR, "", "APPLE", null));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.FOREX_PAIR, "\r\r\r", "APPLE", null));
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.FOREX_PAIR, "asdfghjk", "APPLE", null));
        
        assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.OTHER, "asdfghjk", "APPLE", null));
        assertNotEquals(ai1, ai2);

        // random test for asset type
        assertTrue(ai1.assetType().getDefaultQuantityPrecision() == AssetType.ETF.getDefaultQuantityPrecision());
        assertTrue(ai1.assetType().getDefaultQuantityPrecision().getDecimalPlaces() == 4);
    }
    
    @Test
    void testAssetCommonName() {
        assertTrue(asset.assetCommonName().equals("APPLE"));
    }
    
    @Test
    void testCryptoSymbol() {
        assertEquals("BTC", asset2.primaryIdentifier());
    }
    
    @Test
    void testEquals() {
        AssetIdentifier ai1 = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        assertTrue(ai1.equals(asset));
        assertTrue(ai1.equals(ai1));
        
        assertFalse(asset2.equals(asset));
        assertFalse(asset.equals(null));
        assertFalse(asset.equals(new Object()));
        
        assertFalse(asset.equals(new AssetIdentifier(AssetType.BOND, "APPL", "APPLE", "NASDAQ")));
        assertFalse(asset.equals(new AssetIdentifier(AssetType.STOCK, "BTC", "APPLE", "NASDAQ")));
        assertFalse(asset.equals(new AssetIdentifier(AssetType.STOCK, "APPL", "GOOGLE", "NASDAQ")));
        assertFalse(asset.equals(new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "TSX")));
        // assertFalse(asset.equals(new AssetIdentifier(AssetType.STOCK, "APPL", "APPLZ", "")));
        // assertFalse(asset.equals(new AssetIdentifier(AssetType.STOCK, "APPL", "APPLZ", null)));

        AssetIdentifier ai2 = new AssetIdentifier(AssetType.ETF, "BTC", "bitcoin", "COINGECKO");
        assertFalse(ai2.equals(new AssetIdentifier(AssetType.CRYPTO, "XRP", "bitcoin", "COINGECKO")));
        assertFalse(ai2.equals(new AssetIdentifier(AssetType.CRYPTO, "BTC", "Dogecoin", "COINGECKO")));
        assertFalse(ai2.equals(new AssetIdentifier(AssetType.CRYPTO, "BTC", "bitcoin", "RIPPLE COiN Exchange")));

    }

    @Test
    void testExchange() {
        assertEquals("NASDAQ", asset.secondaryIdentifier());
        assertNotEquals("NASDAQ  ", asset.secondaryIdentifier());
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
        assertTrue(new AssetIdentifier(AssetType.ETF, "VOO", "Vanguard S&P 500", "NASDAQ").isStockOrETF());

    }

    @Test
    void testTickerSymbol() {
        assertEquals("APPL", asset.primaryIdentifier());
        assertNotEquals(" APPL", asset.primaryIdentifier());
        assertNotEquals(" ", asset.primaryIdentifier());
    }

    @Test
    void testToString() {
        assertNotNull(asset.toString());
    }
}
