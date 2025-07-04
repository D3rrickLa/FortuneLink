package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class AssetTransferTransactionDetailsTest {
    // Common test data
    private AssetIdentifier assetIdentifier;
    private BigDecimal assetQuantity;
    private Money costBasisPerUnit;
    private UUID account1Id;
    private UUID account2Id;

    // Helper definitions
    private PortfolioCurrency usdCurrency;

        @BeforeEach
    void init() {
        usdCurrency = new PortfolioCurrency(Currency.getInstance("USD"));
        
        assetIdentifier = new AssetIdentifier(AssetType.STOCK, "Alphabet Inc.", "GOOG", "NASDAQ");
        assetQuantity = new BigDecimal("10.000000"); // Use string constructor for precision
        costBasisPerUnit = new Money(new BigDecimal("150.75"), usdCurrency);

        account1Id = UUID.randomUUID();
        account2Id = UUID.randomUUID();
    }
    private Money createMoney(String amount, PortfolioCurrency currency) {
        return new Money(new BigDecimal(amount), currency);
    }
    
   
    @Test
    void testConstructorGoodValidTransferAccountToAccount() {
        // Arrange
        UUID source = account1Id;
        UUID destination = account2Id;

        // Act
        AssetTransferTransactionDetails details = new AssetTransferTransactionDetails(
            source, destination, assetIdentifier, assetQuantity, costBasisPerUnit
        );

        // Assert
        assertNotNull(details);
        assertEquals(source, details.getSourceAccountId());
        assertEquals(destination, details.getDestinationAccountId());
        assertEquals(assetIdentifier, details.getAssetIdentifier());
        assertEquals(assetQuantity.setScale(6, RoundingMode.HALF_UP), details.getAssetQuantity()); // Check scale
        assertEquals(costBasisPerUnit, details.getCostBasisPerUnit());
    }

    @Test
    void testConstructorGoodValidTransferFromExternalToAccount() {
        // Arrange
        UUID source = null; // From external
        UUID destination = account1Id; // To an account

        // Act
        AssetTransferTransactionDetails details = new AssetTransferTransactionDetails(
            source, destination, assetIdentifier, assetQuantity, costBasisPerUnit
        );

        // Assert
        assertNotNull(details);
        assertNull(details.getSourceAccountId());
        assertEquals(destination, details.getDestinationAccountId());
        assertEquals(assetIdentifier, details.getAssetIdentifier());
        assertEquals(assetQuantity.setScale(6, RoundingMode.HALF_UP), details.getAssetQuantity());
        assertEquals(costBasisPerUnit, details.getCostBasisPerUnit());
    }

    @Test
    void testConstructorGoodValidTransferFromAccountToExternal() {
        // Arrange
        UUID source = account1Id; // From an account
        UUID destination = null; // To external

        // Act
        AssetTransferTransactionDetails details = new AssetTransferTransactionDetails(
            source, destination, assetIdentifier, assetQuantity, costBasisPerUnit
        );

        // Assert
        assertNotNull(details);
        assertEquals(source, details.getSourceAccountId());
        assertNull(details.getDestinationAccountId());
        assertEquals(assetIdentifier, details.getAssetIdentifier());
        assertEquals(assetQuantity.setScale(6, RoundingMode.HALF_UP), details.getAssetQuantity());
        assertEquals(costBasisPerUnit, details.getCostBasisPerUnit());
    }

    // --- Test Constructor - Unhappy Paths (Validation) ---

    @Test
    void testConstructorBadThrowsExceptionIfBothSourceAndDestinationAreNull() {
        // Arrange
        UUID source = null;
        UUID destination = null;

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransferTransactionDetails(
                source, destination, assetIdentifier, assetQuantity, costBasisPerUnit
            );
        });
        assertTrue(thrown.getMessage().contains("Either sourceAccountId or destinationAccountId (or both) must be specified for AssetTransferDetails."));
    }

    @Test
    void testConstructorBadThrowsExceptionForNullAssetIdentifier() {
        // Arrange
        AssetIdentifier nullAssetIdentifier = null;

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new AssetTransferTransactionDetails(
                account1Id, account2Id, nullAssetIdentifier, assetQuantity, costBasisPerUnit
            );
        });
        assertTrue(thrown.getMessage().contains("Asset identifier cannot be null for AssetTransferDetails."));
    }

    @Test
    void testConstructorBadThrowsExceptionForNullAssetQuantity() {
        // Arrange
        BigDecimal nullQuantity = null;

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new AssetTransferTransactionDetails(
                account1Id, account2Id, assetIdentifier, nullQuantity, costBasisPerUnit
            );
        });
        assertTrue(thrown.getMessage().contains("Quantity cannot be null for AssetTransferDetails."));
    }

    @Test
    void testConstructorBadThrowsExceptionForZeroAssetQuantity() {
        // Arrange
        BigDecimal zeroQuantity = BigDecimal.ZERO;

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransferTransactionDetails(
                account1Id, account2Id, assetIdentifier, zeroQuantity, costBasisPerUnit
            );
        });
        assertTrue(thrown.getMessage().contains("Quantity must be positive for AssetTransferDetails."));
    }

    @Test
    void testConstructorBadThrowsExceptionForNegativeAssetQuantity() {
        // Arrange
        BigDecimal negativeQuantity = new BigDecimal("-5.00");

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransferTransactionDetails(
                account1Id, account2Id, assetIdentifier, negativeQuantity, costBasisPerUnit
            );
        });
        assertTrue(thrown.getMessage().contains("Quantity must be positive for AssetTransferDetails."));
    }

    @Test
    void testConstructorBadThrowsExceptionForNullCostBasisPerUnit() {
        // Arrange
        Money nullCostBasis = null;

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new AssetTransferTransactionDetails(
                account1Id, account2Id, assetIdentifier, assetQuantity, nullCostBasis
            );
        });
        assertTrue(thrown.getMessage().contains("Cost Basis Per Unit cannot be null."));
    }

    // You might want to add a check for non-negative costBasisPerUnit if that's a business rule
    // For example, if costBasisPerUnit represents an average cost, it typically shouldn't be negative.
    @Test
    void testConstructor_ThrowsExceptionForNegativeCostBasisPerUnit() {
        // Arrange
        Money negativeCostBasis = createMoney("-10.00", usdCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransferTransactionDetails(
                account1Id, account2Id, assetIdentifier, assetQuantity, negativeCostBasis
            );
        });
        // Assuming you add this validation to your constructor:
        // if (costBasisPerUnit.amount().compareTo(BigDecimal.ZERO) < 0) {
        //     throw new IllegalArgumentException("Cost Basis Per Unit cannot be negative.");
        // }
        assertTrue(thrown.getMessage().contains("Cost Basis Per Unit cannot be negative."));
    } 
    
    
    // --- Test Equals and HashCode ---
    @Test 
    void testEqualsIfBranches() {
        // Arrange
        AssetTransferTransactionDetails details1 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );
        AssetTransferTransactionDetails details2 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );
    
        // Assert
        assertTrue(details1.equals(details1));
        assertTrue(details1.equals(details2));
        assertTrue(details2.equals(details1));
        assertEquals(details1.hashCode(), details2.hashCode());

        assertFalse(details1.equals(new Object()));
        assertFalse(details1.equals(null ));

    }
    
    @Test
    void testEquals_SameObjects() {
        // Arrange
        AssetTransferTransactionDetails details1 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );
        AssetTransferTransactionDetails details2 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );

        // Assert
        assertTrue(details1.equals(details2));
        assertTrue(details2.equals(details1));
        assertEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentSourceAccountId() {
        // Arrange
        AssetTransferTransactionDetails details1 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );
        AssetTransferTransactionDetails details2 = new AssetTransferTransactionDetails(
            UUID.randomUUID(), account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentDestinationAccountId() {
        // Arrange
        AssetTransferTransactionDetails details1 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );
        AssetTransferTransactionDetails details2 = new AssetTransferTransactionDetails(
            account1Id, UUID.randomUUID(), assetIdentifier, assetQuantity, costBasisPerUnit
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentAssetIdentifier() {
        // Arrange
        AssetTransferTransactionDetails details1 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );
        AssetTransferTransactionDetails details2 = new AssetTransferTransactionDetails(
            account1Id, account2Id, new AssetIdentifier(AssetType.STOCK, "Microsoft", "MSFT", "NASDAQ"), assetQuantity, costBasisPerUnit
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentAssetQuantity() {
        // Arrange
        AssetTransferTransactionDetails details1 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );
        AssetTransferTransactionDetails details2 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, new BigDecimal("11.0"), costBasisPerUnit
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentCostBasisPerUnit() {
        // Arrange
        AssetTransferTransactionDetails details1 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );
        AssetTransferTransactionDetails details2 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, createMoney("160.00", usdCurrency)
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_SourceAccountNullVsNonNull() {
        // Arrange
        AssetTransferTransactionDetails details1 = new AssetTransferTransactionDetails(
            null, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );
        AssetTransferTransactionDetails details2 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DestinationAccountNullVsNonNull() {
        // Arrange
        AssetTransferTransactionDetails details1 = new AssetTransferTransactionDetails(
            account1Id, null, assetIdentifier, assetQuantity, costBasisPerUnit
        );
        AssetTransferTransactionDetails details2 = new AssetTransferTransactionDetails(
            account1Id, account2Id, assetIdentifier, assetQuantity, costBasisPerUnit
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }
}
