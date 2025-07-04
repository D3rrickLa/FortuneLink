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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class AssetTransactionDetailsTest {
    // Declared, but initialized per test or in helper methods
    private AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private Money pricePerUnit;
    
    // Helper definitions - good to keep these in init or as static finals
    private PortfolioCurrency usdCurrency;
    private PortfolioCurrency cadCurrency;

    @BeforeEach
    void init() {
        usdCurrency = new PortfolioCurrency(Currency.getInstance("USD"));
        cadCurrency = new PortfolioCurrency(Currency.getInstance("CAD"));
        
        assetIdentifier = new AssetIdentifier(AssetType.STOCK, "APPLE", "AAPL", "NASDAQ");
        quantity = new BigDecimal("20.000000"); // Use string constructor for BigDecimal for precision
        pricePerUnit = new Money(new BigDecimal("213.55"), usdCurrency); // Assume asset trades in USD
    }

    private Money createMoney(String amount, PortfolioCurrency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    @Test 
    void testConstructorGoodBuyTransaction() {
        // Arrange
        TransactionType type = TransactionType.BUY; // BUY
        Money assetValueInAsset = createMoney("4271.00", usdCurrency); // 20 * 213.55
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency); // 4271.00 * 1.45
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        // costBasisOfSoldQuantityInPortfolioCurrency should be NULL for a BUY
        Money costBasisSold = null; 

        // Act
        AssetTransactionDetails details = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold,
            forexFees, otherFees
        );
           // Assert
        assertNotNull(details);
        assertEquals(assetIdentifier, details.getAssetIdentifier());
        assertEquals(quantity.setScale(4, RoundingMode.HALF_UP), details.getQuantity()); // Check scale
        assertEquals(pricePerUnit, details.getPricePerUnit());
        assertEquals(type, details.getTransactionType());
        assertEquals(assetValueInAsset, details.getAssetValueInAssetCurrency());
        assertEquals(assetValueInPortfolio, details.getAssetValueInPortfolioCurrency());
        assertNull(details.getCostBasisOfSoldQuantityInPortfolioCurrency(), "Cost basis of sold quantity must be null for a BUY transaction.");
        assertEquals(forexFees, details.getTotalForexConversionFeesInPortfolioCurrency());
        assertEquals(otherFees, details.getTotalOtherFeesInPortfolioCurrency());
    }

    @Test
    void testConstructorGoodSellTransaction() {
        // Arrange
        TransactionType type = TransactionType.SELL; // SELL
        Money assetValueInAsset = createMoney("4271.00", usdCurrency); // Proceeds in asset currency
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency); // Proceeds in portfolio currency
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        // costBasisOfSoldQuantityInPortfolioCurrency MUST be NOT NULL for a SELL
        Money costBasisSold = createMoney("3000.00", cadCurrency); // Example cost basis for 20 shares

        // Act
        AssetTransactionDetails details = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold,
            forexFees, otherFees
        );

        // Assert
        assertNotNull(details);
        assertEquals(type, details.getTransactionType());
        assertEquals(costBasisSold, details.getCostBasisOfSoldQuantityInPortfolioCurrency(), "Cost basis of sold quantity must be correctly set for a SELL transaction.");
        // Add other assertions similar to the BUY test
    }

    @Test
    void testConstructorBadThrowsExceptionForNegativeQuantity() {
        // Arrange
        BigDecimal negativeQuantity = new BigDecimal("-10.00");
        TransactionType type = TransactionType.BUY;
        Money assetValueInAsset = createMoney("100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("145.00", cadCurrency);
        Money forexFees = createMoney("1.00", cadCurrency);
        Money otherFees = createMoney("2.00", cadCurrency);
        
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransactionDetails(
                assetIdentifier, negativeQuantity, pricePerUnit, type,
                assetValueInAsset, assetValueInPortfolio, null, // null for BUY
                forexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Quantity cannot be negative or zero."));
    }

    @Test
    void testConstructorBadThrowsExceptionForZeroPricePerUnit() {
        // Arrange
        Money zeroPrice = new Money(BigDecimal.ZERO, usdCurrency);
        TransactionType type = TransactionType.BUY;
        Money assetValueInAsset = createMoney("100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("145.00", cadCurrency);
        Money forexFees = createMoney("1.00", cadCurrency);
        Money otherFees = createMoney("2.00", cadCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransactionDetails(
                assetIdentifier, quantity, zeroPrice, type,
                assetValueInAsset, assetValueInPortfolio, null, // null for BUY
                forexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Price of each asset unit must be greater than zero."));
    }

     // --- Specific Validation Tests for costBasisOfSoldQuantityInPortfolioCurrency ---
    @Test
    void testConstructorBadThrowsExceptionIfCostBasisProvidedForBuy() {
        // Arrange
        TransactionType type = TransactionType.BUY; // BUY
        Money assetValueInAsset = createMoney("100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("145.00", cadCurrency);
        Money forexFees = createMoney("1.00", cadCurrency);
        Money otherFees = createMoney("2.00", cadCurrency);
        
        // Providing a non-null cost basis for a BUY, which should throw an error
        Money invalidCostBasis = createMoney("50.00", cadCurrency); 

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransactionDetails(
                assetIdentifier, quantity, pricePerUnit, type,
                assetValueInAsset, assetValueInPortfolio, invalidCostBasis,
                forexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Cost basis of sold quantity must be null for BUY transaction type."));
    }

    @Test
    void testConstructorBadThrowsExceptionIfCostBasisIsNullForSell() {
        // Arrange
        TransactionType type = TransactionType.SELL; // SELL
        Money assetValueInAsset = createMoney("100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("145.00", cadCurrency);
        Money forexFees = createMoney("1.00", cadCurrency);
        Money otherFees = createMoney("2.00", cadCurrency);
        
        // Providing a null cost basis for a SELL, which should throw an error
        Money invalidCostBasis = null; 

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> { // Assuming Objects.requireNonNull for this
            new AssetTransactionDetails(
                assetIdentifier, quantity, pricePerUnit, type,
                assetValueInAsset, assetValueInPortfolio, invalidCostBasis,
                forexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Cost basis of sold quantity cannot be null for SELL transaction type."));
    }
   
    @Test
    void testConstructorBadThrowsExceptionForNegativeAssetValueInAssetCurrency() {
        // Arrange
        TransactionType type = TransactionType.BUY;
        Money assetValueInAsset = createMoney("-100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("145.00", cadCurrency);
        Money negativeForexFees = createMoney("1.00", cadCurrency);
        Money otherFees = createMoney("2.00", cadCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransactionDetails(
                assetIdentifier, quantity, pricePerUnit, type,
                assetValueInAsset, assetValueInPortfolio, null,
                negativeForexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Asset value in asset currency cannot be negative."));
    }

    @Test
    void testConstructorBadThrowsExceptionForNegativeAssetValueInPortfolioCurrency() {
        // Arrange
        TransactionType type = TransactionType.BUY;
        Money assetValueInAsset = createMoney("100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("-145.00", cadCurrency);
        Money negativeForexFees = createMoney("1.00", cadCurrency);
        Money otherFees = createMoney("2.00", cadCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransactionDetails(
                assetIdentifier, quantity, pricePerUnit, type,
                assetValueInAsset, assetValueInPortfolio, null,
                negativeForexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Asset value in portfolio currency cannot be negative."));
    }

    @Test
    void testConstructorBadThrowsExceptionForNegativeForexFees() {
        // Arrange
        TransactionType type = TransactionType.BUY;
        Money assetValueInAsset = createMoney("100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("145.00", cadCurrency);
        Money negativeForexFees = createMoney("-1.00", cadCurrency);
        Money otherFees = createMoney("2.00", cadCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransactionDetails(
                assetIdentifier, quantity, pricePerUnit, type,
                assetValueInAsset, assetValueInPortfolio, null,
                negativeForexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Total FOREX conversion fees cannot be negative."));
    }

    @Test
    void testConstructorBadThrowsExceptionForNegativeOtherFees() {
        // Arrange
        TransactionType type = TransactionType.BUY;
        Money assetValueInAsset = createMoney("100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("145.00", cadCurrency);
        Money negativeForexFees = createMoney("2.00", cadCurrency);
        Money otherFees = createMoney("-2.00", cadCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransactionDetails(
                assetIdentifier, quantity, pricePerUnit, type,
                assetValueInAsset, assetValueInPortfolio, null,
                negativeForexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Total other fees cannot be negative."));
    }

    @Test
    void testConstructorBadThrowsExceptionForWrongTransactionType() {
        // Arrange
        TransactionType type = TransactionType.EXPENSE;
        Money assetValueInAsset = createMoney("100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("145.00", cadCurrency);
        Money negativeForexFees = createMoney("2.00", cadCurrency);
        Money otherFees = createMoney("2.00", cadCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new AssetTransactionDetails(
                assetIdentifier, quantity, pricePerUnit, type,
                assetValueInAsset, assetValueInPortfolio, null,
                negativeForexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Invalid transaction type for AssetTransactionDetails: " + type));
    }


    @Test 
    void testConstructorBadThrowsExceptionForNegativeCostBasis() {
        // Arrange
        TransactionType type = TransactionType.SELL; // SELL
        Money assetValueInAsset = createMoney("100.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("145.00", cadCurrency);
        Money forexFees = createMoney("1.00", cadCurrency);
        Money otherFees = createMoney("2.00", cadCurrency);
        
        // Providing a null cost basis for a SELL, which should throw an error
        Money invalidCostBasis = createMoney("-3000.00", cadCurrency); // Example cost basis for 20 shares

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> { // Assuming Objects.requireNonNull for this
            new AssetTransactionDetails(
                assetIdentifier, quantity, pricePerUnit, type,
                assetValueInAsset, assetValueInPortfolio, invalidCostBasis,
                forexFees, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Cost basis of sold quantity cannot be negative for SELL."));
    }



    // --- Test Equals and HashCode ---
    @Test
    void testEqualsIfCatches() {
        // Arrange
        TransactionType type = TransactionType.BUY;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, null,
            forexFees, otherFees
        );
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, null,
            forexFees, otherFees
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
        TransactionType type = TransactionType.BUY;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, null,
            forexFees, otherFees
        );
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, null,
            forexFees, otherFees
        );

        // Assert
        assertTrue(details1.equals(details2));
        assertTrue(details2.equals(details1));
        assertEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentAssetIdentifier() {
        // Arrange
        TransactionType type = TransactionType.BUY;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, null,
            forexFees, otherFees
        );
        
        AssetIdentifier differentAssetIdentifier = new AssetIdentifier(AssetType.STOCK, "MSFT", "MSFT", "NASDAQ");
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            differentAssetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, null,
            forexFees, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentTransactionType() {
        // Arrange common data
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        // Details for a BUY
        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, TransactionType.BUY,
            assetValueInAsset, assetValueInPortfolio, null,
            forexFees, otherFees
        );

        // Details for a SELL with same other data
        Money costBasisSold = createMoney("3000.00", cadCurrency);
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, TransactionType.SELL,
            assetValueInAsset, assetValueInPortfolio, costBasisSold, // Ensure costBasisSold is handled correctly in equals/hashCode for SELLs
            forexFees, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    // Remember to test equals/hashCode for all fields, including costBasisOfSoldQuantityInPortfolioCurrency
    @Test
    void testEquals_DifferentCostBasisOfSoldQuantity() {
        // Arrange
        TransactionType type = TransactionType.SELL;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        Money costBasisSold1 = createMoney("3000.00", cadCurrency);
        Money costBasisSold2 = createMoney("3100.00", cadCurrency); // Different cost basis

        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold1,
            forexFees, otherFees
        );
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold2,
            forexFees, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentQuantity() {
        // Arrange
        TransactionType type = TransactionType.SELL;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        Money costBasisSold1 = createMoney("3000.00", cadCurrency);
        Money costBasisSold2 = createMoney("3000.00", cadCurrency); // Different cost basis

        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold1,
            forexFees, otherFees
        );
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity.multiply(new BigDecimal(2)), pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold2,
            forexFees, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentPricePerUnit() {
        // Arrange
        TransactionType type = TransactionType.SELL;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        Money costBasisSold1 = createMoney("3000.00", cadCurrency);
        Money costBasisSold2 = createMoney("3000.00", cadCurrency); // Different cost basis

        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold1,
            forexFees, otherFees
        );
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit.multiply(new BigDecimal(2)), type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold2,
            forexFees, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentAssetValueInAssetCurrency() {
        // Arrange
        TransactionType type = TransactionType.SELL;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        Money costBasisSold1 = createMoney("3000.00", cadCurrency);
        Money costBasisSold2 = createMoney("3000.00", cadCurrency); // Different cost basis

        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold1,
            forexFees, otherFees
        );
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset.multiply(new BigDecimal(2)), assetValueInPortfolio, costBasisSold2,
            forexFees, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }
    
    @Test
    void testEquals_DifferentAssetValueInPorfolioCurrency() {
        // Arrange
        TransactionType type = TransactionType.SELL;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        Money costBasisSold1 = createMoney("3000.00", cadCurrency);
        Money costBasisSold2 = createMoney("3000.00", cadCurrency); // Different cost basis

        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold1,
            forexFees, otherFees
        );
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio.multiply(new BigDecimal(2)), costBasisSold2,
            forexFees, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }
    @Test
    void testEquals_DifferentForexCost() {
        // Arrange
        TransactionType type = TransactionType.SELL;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        Money costBasisSold1 = createMoney("3000.00", cadCurrency);
        Money costBasisSold2 = createMoney("3000.00", cadCurrency); // Different cost basis

        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold1,
            forexFees, otherFees
        );
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold2,
            forexFees.multiply(new BigDecimal(2)), otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }
    @Test
    void testEquals_DifferentOtherFees() {
        // Arrange
        TransactionType type = TransactionType.SELL;
        Money assetValueInAsset = createMoney("4271.00", usdCurrency);
        Money assetValueInPortfolio = createMoney("6192.95", cadCurrency);
        Money forexFees = createMoney("5.00", cadCurrency);
        Money otherFees = createMoney("12.54", cadCurrency);
        
        Money costBasisSold1 = createMoney("3000.00", cadCurrency);
        Money costBasisSold2 = createMoney("3000.00", cadCurrency); // Different cost basis

        AssetTransactionDetails details1 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold1,
            forexFees, otherFees
        );
        AssetTransactionDetails details2 = new AssetTransactionDetails(
            assetIdentifier, quantity, pricePerUnit, type,
            assetValueInAsset, assetValueInPortfolio, costBasisSold2,
            forexFees, otherFees.multiply(new BigDecimal(2))
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }
}
