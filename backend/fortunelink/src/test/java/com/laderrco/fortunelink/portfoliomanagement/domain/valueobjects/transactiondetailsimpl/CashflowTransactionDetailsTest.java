package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class CashflowTransactionDetailsTest {
    
    // Helper currency definitions
    private PortfolioCurrency usdCurrency;
    private PortfolioCurrency cadCurrency;

    @BeforeEach
    void init() {
        usdCurrency = new PortfolioCurrency(Currency.getInstance("USD"));
        cadCurrency = new PortfolioCurrency(Currency.getInstance("CAD"));
    }

    // --- Helper Method for creating Money objects ---
    private Money createMoney(String amount, PortfolioCurrency currency) {
        return new Money(new BigDecimal(amount), currency);
    }
    
  @Test
    void testConstructor_ValidCashflow_WithFees() {
        // Arrange
        Money originalAmount = createMoney("100.00", usdCurrency);
        Money convertedAmount = createMoney("135.00", cadCurrency); // 100 * 1.35
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFee = createMoney("2.50", cadCurrency);
        Money otherFees = createMoney("5.00", cadCurrency);

        // Act
        CashflowTransactionDetails details = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee, otherFees
        );

        // Assert
        assertNotNull(details);
        assertEquals(originalAmount, details.getOriginalCashflowAmount());
        assertEquals(convertedAmount, details.getConvertedCashflowAmount());
        assertEquals(exchangeRate, details.getExchangeRate());
        assertEquals(exchangeRateFee, details.getExchangeRateFee());
        assertEquals(otherFees, details.getOtherFees());
    }

    @Test
    void testConstructor_ValidCashflow_NoFees() {
        // Arrange
        Money originalAmount = createMoney("50.00", usdCurrency);
        Money convertedAmount = createMoney("67.50", cadCurrency);
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFee = createMoney("0.00", cadCurrency); // No fee
        Money otherFees = createMoney("0.00", cadCurrency); // No fee

        // Act
        CashflowTransactionDetails details = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee, otherFees
        );

        // Assert
        assertNotNull(details);
        assertEquals(originalAmount, details.getOriginalCashflowAmount());
        assertEquals(convertedAmount, details.getConvertedCashflowAmount());
        assertEquals(exchangeRate, details.getExchangeRate());
        assertEquals(exchangeRateFee, details.getExchangeRateFee());
        assertEquals(otherFees, details.getOtherFees());
    }

    // --- Test Constructor - Unhappy Paths (Validation) ---

    @Test
    void testConstructor_ThrowsExceptionForNullOriginalCashflowAmount() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new CashflowTransactionDetails(
                null, createMoney("100.00", cadCurrency), new BigDecimal("1.0"),
                createMoney("0.00", cadCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Original cashflow amount cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullConvertedCashflowAmount() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("100.00", usdCurrency), null, new BigDecimal("1.0"),
                createMoney("0.00", cadCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Converted cashflow amount cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullExchangeRate() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("100.00", usdCurrency), createMoney("100.00", cadCurrency), null,
                createMoney("0.00", cadCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Exchange rate cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullExchangeRateFee() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("100.00", usdCurrency), createMoney("100.00", cadCurrency), new BigDecimal("1.0"),
                null, createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Exchange rate fee cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullOtherFees() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("100.00", usdCurrency), createMoney("100.00", cadCurrency), new BigDecimal("1.0"),
                createMoney("0.00", cadCurrency), null
            );
        });
        assertTrue(thrown.getMessage().contains("Other fees amount cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForZeroOriginalCashflowAmount() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("0.00", usdCurrency), createMoney("100.00", cadCurrency), new BigDecimal("1.0"),
                createMoney("0.00", cadCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Original cashflow amount must be positive."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativeOriginalCashflowAmount() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("-10.00", usdCurrency), createMoney("100.00", cadCurrency), new BigDecimal("1.0"),
                createMoney("0.00", cadCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Original cashflow amount must be positive."));
    }

    @Test
    void testConstructor_ThrowsExceptionForZeroConvertedCashflowAmount() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("100.00", usdCurrency), createMoney("0.00", cadCurrency), new BigDecimal("1.0"),
                createMoney("0.00", cadCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Converted cashflow amount must be positive."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativeConvertedCashflowAmount() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("100.00", usdCurrency), createMoney("-10.00", cadCurrency), new BigDecimal("1.0"),
                createMoney("0.00", cadCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Converted cashflow amount must be positive."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativeExchangeRate() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("100.00", usdCurrency), createMoney("100.00", cadCurrency), new BigDecimal("-0.5"),
                createMoney("0.00", cadCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Exchange rate must be positive."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativeExchangeRateFee() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("100.00", usdCurrency), createMoney("100.00", cadCurrency), new BigDecimal("1.0"),
                createMoney("-1.00", cadCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Fee amounts cannot be negative."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativeOtherFees() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CashflowTransactionDetails(
                createMoney("100.00", usdCurrency), createMoney("100.00", cadCurrency), new BigDecimal("1.0"),
                createMoney("0.00", cadCurrency), createMoney("-1.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Fee amounts cannot be negative."));
    }

    @Test
    void testConstructor_ThrowsExceptionForCurrencyMismatch_FeesAndConvertedAmount() {
        // Arrange
        Money originalAmount = createMoney("100.00", usdCurrency);
        Money convertedAmount = createMoney("135.00", cadCurrency);
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFeeInUSD = createMoney("2.50", usdCurrency); // Wrong currency!
        Money otherFees = createMoney("5.00", cadCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CashflowTransactionDetails(
                originalAmount, convertedAmount, exchangeRate, exchangeRateFeeInUSD, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Converted cashflow amount and all fees in CashTransactionDetails must be in the same portfolio currency."));
    }

    @Test
    void testConstructor_ThrowsExceptionForCurrencyMismatch_OtherFees() {
        // Arrange
        Money originalAmount = createMoney("100.00", usdCurrency);
        Money convertedAmount = createMoney("135.00", cadCurrency);
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFeeInUSD = createMoney("2.50", cadCurrency); 
        Money otherFees = createMoney("5.00", usdCurrency); // Wrong currency!

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new CashflowTransactionDetails(
                originalAmount, convertedAmount, exchangeRate, exchangeRateFeeInUSD, otherFees
            );
        });
        assertTrue(thrown.getMessage().contains("Converted cashflow amount and all fees in CashTransactionDetails must be in the same portfolio currency."));
    }

    // --- Test Equals and HashCode ---

    @Test 
    void testEqualIfBranches() {
        // Arrange
        Money originalAmount = createMoney("100.00", usdCurrency);
        Money convertedAmount = createMoney("135.00", cadCurrency);
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFee = createMoney("2.50", cadCurrency);
        Money otherFees = createMoney("5.00", cadCurrency);
    
        CashflowTransactionDetails details1 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee, otherFees
        );
        CashflowTransactionDetails details2 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee, otherFees
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
        Money originalAmount = createMoney("100.00", usdCurrency);
        Money convertedAmount = createMoney("135.00", cadCurrency);
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFee = createMoney("2.50", cadCurrency);
        Money otherFees = createMoney("5.00", cadCurrency);

        CashflowTransactionDetails details1 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee, otherFees
        );
        CashflowTransactionDetails details2 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee, otherFees
        );

        // Assert
        assertTrue(details1.equals(details2));
        assertTrue(details2.equals(details1));
        assertEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentOriginalCashflowAmount() {
        // Arrange
        Money originalAmount1 = createMoney("100.00", usdCurrency);
        Money originalAmount2 = createMoney("110.00", usdCurrency);
        Money convertedAmount = createMoney("135.00", cadCurrency);
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFee = createMoney("2.50", cadCurrency);
        Money otherFees = createMoney("5.00", cadCurrency);

        CashflowTransactionDetails details1 = new CashflowTransactionDetails(
            originalAmount1, convertedAmount, exchangeRate, exchangeRateFee, otherFees
        );
        CashflowTransactionDetails details2 = new CashflowTransactionDetails(
            originalAmount2, convertedAmount, exchangeRate, exchangeRateFee, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentConvertedCashflowAmount() {
        // Arrange
        Money originalAmount = createMoney("100.00", usdCurrency);
        Money convertedAmount1 = createMoney("135.00", cadCurrency);
        Money convertedAmount2 = createMoney("140.00", cadCurrency);
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFee = createMoney("2.50", cadCurrency);
        Money otherFees = createMoney("5.00", cadCurrency);

        CashflowTransactionDetails details1 = new CashflowTransactionDetails(
            originalAmount, convertedAmount1, exchangeRate, exchangeRateFee, otherFees
        );
        CashflowTransactionDetails details2 = new CashflowTransactionDetails(
            originalAmount, convertedAmount2, exchangeRate, exchangeRateFee, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentExchangeRate() {
        // Arrange
        Money originalAmount = createMoney("100.00", usdCurrency);
        Money convertedAmount = createMoney("135.00", cadCurrency);
        BigDecimal exchangeRate1 = new BigDecimal("1.35");
        BigDecimal exchangeRate2 = new BigDecimal("1.40");
        Money exchangeRateFee = createMoney("2.50", cadCurrency);
        Money otherFees = createMoney("5.00", cadCurrency);

        CashflowTransactionDetails details1 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate1, exchangeRateFee, otherFees
        );
        CashflowTransactionDetails details2 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate2, exchangeRateFee, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentExchangeRateFee() {
        // Arrange
        Money originalAmount = createMoney("100.00", usdCurrency);
        Money convertedAmount = createMoney("135.00", cadCurrency);
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFee1 = createMoney("2.50", cadCurrency);
        Money exchangeRateFee2 = createMoney("3.00", cadCurrency);
        Money otherFees = createMoney("5.00", cadCurrency);

        CashflowTransactionDetails details1 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee1, otherFees
        );
        CashflowTransactionDetails details2 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee2, otherFees
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentOtherFees() {
        // Arrange
        Money originalAmount = createMoney("100.00", usdCurrency);
        Money convertedAmount = createMoney("135.00", cadCurrency);
        BigDecimal exchangeRate = new BigDecimal("1.35");
        Money exchangeRateFee = createMoney("2.50", cadCurrency);
        Money otherFees1 = createMoney("5.00", cadCurrency);
        Money otherFees2 = createMoney("6.00", cadCurrency);

        CashflowTransactionDetails details1 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee, otherFees1
        );
        CashflowTransactionDetails details2 = new CashflowTransactionDetails(
            originalAmount, convertedAmount, exchangeRate, exchangeRateFee, otherFees2
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }
}
