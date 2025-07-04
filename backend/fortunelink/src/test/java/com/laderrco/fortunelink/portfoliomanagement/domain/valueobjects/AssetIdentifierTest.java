package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.AssetType;

public class AssetIdentifierTest {
    private AssetType assetType;
    private String assetCommonName;
    private String primaryIdentifier;
    private String secondaryIdentifier;
    private AssetIdentifier testAssetIdentifier01;
    
    @BeforeEach
    void init() {
        assetType = AssetType.STOCK;
        assetCommonName = "APPLE";
        primaryIdentifier = "APPL";
        secondaryIdentifier = "NASDAQ";
        // testAssetIdentifier01 = new AssetIdentifier(assetType, assetCommonName, primaryIdentifier, secondaryIdentifier);
    }

    @Test
    void testConstructorGood() {
        testAssetIdentifier01 = new AssetIdentifier(assetType, assetCommonName, primaryIdentifier, secondaryIdentifier);
        assertNotNull(assetCommonName);
        new AssetIdentifier(AssetType.BOND, assetCommonName, primaryIdentifier, secondaryIdentifier);
        new AssetIdentifier(AssetType.CRYPTO, assetCommonName, primaryIdentifier, secondaryIdentifier);
        new AssetIdentifier(AssetType.COMMODITY, assetCommonName, primaryIdentifier, secondaryIdentifier);
        new AssetIdentifier(AssetType.FOREX_PAIR, assetCommonName, primaryIdentifier, secondaryIdentifier);
        assertTrue(testAssetIdentifier01.assetType().getDefaultQuantityPrecision().getDecimalPlaces() == 4);
    }
    
    @Test 
    void testConstructorBadAssetNameBlank() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(assetType, "\r\r\r", primaryIdentifier, secondaryIdentifier));
        assertEquals("Asset name cannot be blank.", e1.getMessage());
    }
    
    @Test 
    void testConstructorBadSwitchStocks() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(assetType, "APPLE", "\r\r\r", secondaryIdentifier));
        assertEquals("Stock/ETF must have a ticker symbol as primary identifier.", e1.getMessage());
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(assetType, "APPLE", primaryIdentifier, "\r\r\r"));
        assertEquals("Stock/ETF must have an exchange as secondary identifier.", e2.getMessage());
        e2 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(assetType, "APPLE", primaryIdentifier, null));
        assertEquals("Stock/ETF must have an exchange as secondary identifier.", e2.getMessage());
    }

    @Test 
    void testConstructorBadSwitchBond() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.BOND, "APPLE", "\r\r\r", secondaryIdentifier));
        assertEquals("Bond must have a CUSIP/ISIN or unique ID as primary identifier.", e1.getMessage());
    }

    @Test 
    void testConstructorBadSwitchCrypto() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.CRYPTO, "APPLE", "\r\r\r", secondaryIdentifier));
        assertEquals("Cryptocurrency must have a symbol as primary identifier.", e1.getMessage());
    }
    
    @Test 
    void testConstructorBadSwitchCommodity() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.COMMODITY, "APPLE", "\r\r\r", secondaryIdentifier));
        assertEquals("Commodity must have a symbol/code as primary identifier.", e1.getMessage());
    }

    @Test 
    void testConstructorBadSwitchForex() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.FOREX_PAIR, "APPLE", "\r\r\r", secondaryIdentifier));
        assertEquals("Forex pair must have a valid 6-letter symbol.", e1.getMessage());
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.FOREX_PAIR, "APPLE", "1235dsfaa",  null));
        assertEquals("Forex pair must have a valid 6-letter symbol.", e2.getMessage());
    }
    
    @Test
    void testConstructorBadSwitchBadInput() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new AssetIdentifier(AssetType.OTHER, "APPLE", "\r\r\r", secondaryIdentifier));
        assertEquals("ERROR, unknown Asset Type given.", e1.getMessage());
    }
    
    @Test
    void testIsCrypto() {
        testAssetIdentifier01 = new AssetIdentifier(assetType, assetCommonName, primaryIdentifier, secondaryIdentifier);
        assertFalse(testAssetIdentifier01.isCrypto());
        testAssetIdentifier01 = new AssetIdentifier(AssetType.CRYPTO, assetCommonName, primaryIdentifier, secondaryIdentifier);
        assertTrue(testAssetIdentifier01.isCrypto());
    }
    
    @Test
    void testIsStockOrETF() {
        testAssetIdentifier01 = new AssetIdentifier(assetType, assetCommonName, primaryIdentifier, secondaryIdentifier);
        assertTrue(testAssetIdentifier01.isStockOrETF());
        testAssetIdentifier01 = new AssetIdentifier(AssetType.ETF, assetCommonName, primaryIdentifier, secondaryIdentifier);
        assertTrue(testAssetIdentifier01.isStockOrETF());
    }
}
