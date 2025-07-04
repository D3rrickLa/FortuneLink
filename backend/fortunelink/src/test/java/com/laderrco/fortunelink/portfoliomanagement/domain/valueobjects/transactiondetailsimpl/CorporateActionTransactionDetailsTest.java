package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.AssetType;

public class CorporateActionTransactionDetailsTest {
// Common test data
    private AssetIdentifier stockAssetIdentifier;
    private AssetIdentifier etfAssetIdentifier;
    private AssetIdentifier bondAssetIdentifier; // For invalid type testing
    private AssetIdentifier cashAssetIdentifier; // For invalid type testing

    @BeforeEach
    void init() {
        stockAssetIdentifier = new AssetIdentifier(AssetType.STOCK, "Tesla Inc.", "TSLA", "NASDAQ");
        etfAssetIdentifier = new AssetIdentifier(AssetType.ETF, "SPDR S&P 500 ETF Trust", "SPY", "ARCA");
        bondAssetIdentifier = new AssetIdentifier(AssetType.BOND, "US Treasury Bond", "USTB", "OTC");
        cashAssetIdentifier = new AssetIdentifier(AssetType.COMMODITY, "US Dollar Cash", "USD", "FX");
    }

      // --- Test Constructor - Happy Paths ---

    @Test
    void testConstructor_ValidStockSplit() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("2.0"); // 2-for-1 split

        // Act
        CorporateActionTransactionDetails details = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio
        );

        // Assert
        assertNotNull(details);
        assertEquals(stockAssetIdentifier, details.getAssetIdentifier());
        assertEquals(splitRatio, details.getSplitRatio());
    }

    @Test
    void testConstructor_ValidEtfSplit() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("3.0"); // 3-for-1 split

        // Act
        CorporateActionTransactionDetails details = new CorporateActionTransactionDetails(
            etfAssetIdentifier, splitRatio
        );

        // Assert
        assertNotNull(details);
        assertEquals(etfAssetIdentifier, details.getAssetIdentifier());
        assertEquals(splitRatio, details.getSplitRatio());
    }

    @Test
    void testConstructor_ValidReverseSplit() {
        // Arrange
        BigDecimal reverseSplitRatio = new BigDecimal("0.5"); // 1-for-2 reverse split

        // Act
        CorporateActionTransactionDetails details = new CorporateActionTransactionDetails(
            stockAssetIdentifier, reverseSplitRatio
        );

        // Assert
        assertNotNull(details);
        assertEquals(stockAssetIdentifier, details.getAssetIdentifier());
        assertEquals(reverseSplitRatio, details.getSplitRatio());
    }

    // --- Test Constructor - Unhappy Paths (Validation) ---

    @Test
    void testConstructor_ThrowsExceptionForNullAssetIdentifier() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("2.0");

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new CorporateActionTransactionDetails(null, splitRatio);
        });
        assertTrue(thrown.getMessage().contains("Asset Identifier cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullSplitRatio() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new CorporateActionTransactionDetails(stockAssetIdentifier, null);
        });
        assertTrue(thrown.getMessage().contains("Split ratio cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForZeroSplitRatio() {
        // Arrange
        BigDecimal zeroSplitRatio = BigDecimal.ZERO;

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CorporateActionTransactionDetails(stockAssetIdentifier, zeroSplitRatio);
        });
        assertTrue(thrown.getMessage().contains("Split ratio must be positive."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativeSplitRatio() {
        // Arrange
        BigDecimal negativeSplitRatio = new BigDecimal("-1.0");

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CorporateActionTransactionDetails(stockAssetIdentifier, negativeSplitRatio);
        });
        assertTrue(thrown.getMessage().contains("Split ratio must be positive."));
    }

    @Test
    void testConstructor_ThrowsExceptionForInvalidAssetType_Bond() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("2.0");

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CorporateActionTransactionDetails(bondAssetIdentifier, splitRatio);
        });
        assertTrue(thrown.getMessage().contains("Corporate action (like a split) is only applicable to Stock or ETF asset types."));
    }

    @Test
    void testConstructor_ThrowsExceptionForInvalidAssetType_Cash() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("2.0");

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CorporateActionTransactionDetails(cashAssetIdentifier, splitRatio);
        });
        assertTrue(thrown.getMessage().contains("Corporate action (like a split) is only applicable to Stock or ETF asset types."));
    }

    // --- Test Equals and HashCode ---

    @Test 
    void testEqualsIfBranches() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("2.0");
        CorporateActionTransactionDetails details1 = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio
        );
        CorporateActionTransactionDetails details2 = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio
        );
    
        // Assert
        assertTrue(details1.equals(details1));
        assertTrue(details1.equals(details2));
        assertTrue(details2.equals(details1));
        assertEquals(details1.hashCode(), details2.hashCode());

        assertFalse(details1.equals(new Object()));
        assertFalse(details1.equals(null));
    }

    @Test
    void testEquals_SameObjects() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("2.0");
        CorporateActionTransactionDetails details1 = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio
        );
        CorporateActionTransactionDetails details2 = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio
        );

        // Assert
        assertTrue(details1.equals(details2));
        assertTrue(details2.equals(details1));
        assertEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentAssetIdentifier() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("2.0");
        CorporateActionTransactionDetails details1 = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio
        );
        CorporateActionTransactionDetails details2 = new CorporateActionTransactionDetails(
            etfAssetIdentifier, splitRatio
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentSplitRatio() {
        // Arrange
        BigDecimal splitRatio1 = new BigDecimal("2.0");
        BigDecimal splitRatio2 = new BigDecimal("3.0");
        CorporateActionTransactionDetails details1 = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio1
        );
        CorporateActionTransactionDetails details2 = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio2
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_NullAndNonNullComparison() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("2.0");
        CorporateActionTransactionDetails details1 = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio
        );

        // Assert
        assertFalse(details1.equals(null));
    }

    @Test
    void testEquals_DifferentClass() {
        // Arrange
        BigDecimal splitRatio = new BigDecimal("2.0");
        CorporateActionTransactionDetails details = new CorporateActionTransactionDetails(
            stockAssetIdentifier, splitRatio
        );
        Object other = new Object(); // An object of a different class

        // Assert
        assertFalse(details.equals(other));
    }

}
