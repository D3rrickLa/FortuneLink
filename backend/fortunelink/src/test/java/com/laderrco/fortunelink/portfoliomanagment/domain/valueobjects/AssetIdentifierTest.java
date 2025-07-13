package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.CryptoSymbols;

public class AssetIdentifierTest {
    AssetType assetType;
    String symbol = "APPL";
    
    @BeforeEach
    void init() {
        assetType = AssetType.STOCK;
    }

    @Test
    void testConstructorInValidNameIsBlank() {
        String isin ="US0378331005";
        String name = "\r\r\r";
        String exchange = "NASDAQ";
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(symbol, assetType, isin, name, exchange, null));
        assertTrue(e1.getMessage().equals("Asset name cannot be blank."));
    }
    
    @Test
    void testConstructorInValidISIN() {
        String isin ="US037833100";
        String name = "APPLE";
        String exchange = "NASDAQ";
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(symbol, assetType, isin, name, exchange, null));
        assertTrue(e1.getMessage().equals("Invalid ISIN format."));
    }

    @Test
    void testIsCrypto() {
        String isin ="US0378331005";
        String name = "Apple";
        String exchange = "NASDAQ";
        AssetIdentifier identifier = new AssetIdentifier(symbol, assetType, isin, name, exchange, null);
        assertFalse(identifier.isCrypto());
        
        assertFalse(CryptoSymbols.isCrypto("APPL"));
        assertTrue(CryptoSymbols.isCrypto("BTC"));

        AssetIdentifier identifier2 = new AssetIdentifier(symbol, AssetType.CRYPTO, isin, name, exchange, null);
        assertTrue(identifier2.isCrypto());
    }
    
    @Test
    void testIsStockOrETF() {
        String isin ="US0378331005";
        String name = "Apple";
        String exchange = "NASDAQ";
        AssetIdentifier identifier = new AssetIdentifier(symbol, assetType, isin, name, exchange, null);
        assertTrue(identifier.isStockOrETF());
        identifier = new AssetIdentifier(symbol, AssetType.ETF, isin, name, exchange, null);
        assertTrue(identifier.isStockOrETF());

        AssetIdentifier identifier2 = new AssetIdentifier(symbol, AssetType.CRYPTO, isin, name, exchange, null);
        assertFalse(identifier2.isStockOrETF());

    }
}
