package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.AssetType;

public class CorporateActionTransactionDetailsTest {
    
    @Test 
    void testConstructor() {
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        CorporateActionTransactionDetails corporateActionTransactionDetails1 = new CorporateActionTransactionDetails(assetIdentifier, new BigDecimal(3));
        CorporateActionTransactionDetails corporateActionTransactionDetails2 = new CorporateActionTransactionDetails(assetIdentifier, new BigDecimal(2));
        CorporateActionTransactionDetails corporateActionTransactionDetails3 = new CorporateActionTransactionDetails(assetIdentifier, new BigDecimal(3));
        
        assertThrows(NullPointerException.class, () -> new CorporateActionTransactionDetails(null, new BigDecimal(3)));
        assertThrows(NullPointerException.class, () -> new CorporateActionTransactionDetails(null, null));
        
        assertNotEquals(corporateActionTransactionDetails1, corporateActionTransactionDetails2);
        assertEquals(corporateActionTransactionDetails1.getAssetIdentifier(), corporateActionTransactionDetails3.getAssetIdentifier());
        assertEquals(corporateActionTransactionDetails1.getSplitRatio(), corporateActionTransactionDetails3.getSplitRatio());
    }
    
    @Test
    void testEquals() {
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        CorporateActionTransactionDetails corporateActionTransactionDetails1 = new CorporateActionTransactionDetails(assetIdentifier, new BigDecimal(3));
        CorporateActionTransactionDetails corporateActionTransactionDetails2 = new CorporateActionTransactionDetails(assetIdentifier, new BigDecimal(2));
        CorporateActionTransactionDetails corporateActionTransactionDetails3 = new CorporateActionTransactionDetails(assetIdentifier, new BigDecimal(3));
        
        assertTrue(corporateActionTransactionDetails1.equals(corporateActionTransactionDetails3));
        assertTrue(corporateActionTransactionDetails1.equals(corporateActionTransactionDetails1));
        assertFalse(corporateActionTransactionDetails1.equals(corporateActionTransactionDetails2));
        assertFalse(corporateActionTransactionDetails1.equals(null));
        assertFalse(corporateActionTransactionDetails1.equals(new Object()));
        assertFalse(corporateActionTransactionDetails1.equals(new CorporateActionTransactionDetails(assetIdentifier, null)));
        assertFalse(corporateActionTransactionDetails1.equals(new CorporateActionTransactionDetails(new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "TSX") , null)));
        
    }
    
    @Test
    void testHashCode() {
        AssetIdentifier assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPL", "APPLE", "NASDAQ");
        CorporateActionTransactionDetails corporateActionTransactionDetails1 = new CorporateActionTransactionDetails(assetIdentifier, new BigDecimal(3));
        CorporateActionTransactionDetails corporateActionTransactionDetails2 = new CorporateActionTransactionDetails(assetIdentifier, new BigDecimal(2));
        CorporateActionTransactionDetails corporateActionTransactionDetails3 = new CorporateActionTransactionDetails(assetIdentifier, new BigDecimal(3));

        assertTrue(corporateActionTransactionDetails1.hashCode() == corporateActionTransactionDetails3.hashCode());
        assertFalse(corporateActionTransactionDetails1.hashCode() == corporateActionTransactionDetails2.hashCode());
    }
}
