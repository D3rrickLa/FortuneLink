package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Percentage;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class LiabilityIncurrenceTransactionDetailsTest {
     // Common test data
    private PortfolioCurrency usdCurrency;
    private Money originalLoanAmount;
    private Percentage interestPercentage;
    private Instant maturityDate;

    @BeforeEach
    void init() {
        usdCurrency = new PortfolioCurrency(Currency.getInstance("USD"));
        originalLoanAmount = new Money(new BigDecimal("10000.00"), usdCurrency);
        interestPercentage = new Percentage(new BigDecimal("0.05")); // 5%
        maturityDate = Instant.now().plus(365, ChronoUnit.DAYS); // One year from now
    }

    // --- Helper Method for creating Money objects ---
    private Money createMoney(String amount, PortfolioCurrency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    // --- Helper Method for creating Percentage objects ---
    private Percentage createPercentage(String value) {
        return new Percentage(new BigDecimal(value));
    }

    // --- Test Constructor - Happy Paths ---

    @Test
    void testConstructor_ValidDetails() {
        // Act
        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, interestPercentage, maturityDate
        );

        // Assert
        assertNotNull(details);
        assertEquals(originalLoanAmount, details.getOriginalLoanAmount());
        assertEquals(interestPercentage, details.getInterestPercentage());
        assertEquals(maturityDate, details.getMaturityDate());
    }

    @Test
    void testConstructor_ValidDetails_ZeroInterest() {
        // Arrange
        Percentage zeroInterest = createPercentage("0.00"); // 0% interest

        // Act
        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, zeroInterest, maturityDate
        );

        // Assert
        assertNotNull(details);
        assertEquals(zeroInterest, details.getInterestPercentage());
    }

    // --- Test Constructor - Unhappy Paths (Validation) ---

    @Test
    void testConstructor_ThrowsExceptionForNullOriginalLoanAmount() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new LiabilityIncurrenceTransactionDetails(null, interestPercentage, maturityDate);
        });
        assertTrue(thrown.getMessage().contains("originalLoanAmount cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullInterestPercentage() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new LiabilityIncurrenceTransactionDetails(originalLoanAmount, null, maturityDate);
        });
        assertTrue(thrown.getMessage().contains("interestPercentage cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullMaturityDate() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new LiabilityIncurrenceTransactionDetails(originalLoanAmount, interestPercentage, null);
        });
        assertTrue(thrown.getMessage().contains("maturityDate cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForZeroOriginalLoanAmount() {
        // Arrange
        Money zeroAmount = createMoney("0.00", usdCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityIncurrenceTransactionDetails(zeroAmount, interestPercentage, maturityDate);
        });
        assertTrue(thrown.getMessage().contains("Liability iniital amount must be greater than zero."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativeOriginalLoanAmount() {
        // Arrange
        Money negativeAmount = createMoney("-100.00", usdCurrency);

        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityIncurrenceTransactionDetails(negativeAmount, interestPercentage, maturityDate);
        });
        assertTrue(thrown.getMessage().contains("Liability iniital amount must be greater than zero."));
    }

    // --- Test Equals and HashCode ---

    @Test
    void testEquals_SameObjects() {
        // Arrange
        LiabilityIncurrenceTransactionDetails details1 = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, interestPercentage, maturityDate
        );
        LiabilityIncurrenceTransactionDetails details2 = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, interestPercentage, maturityDate
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
    void testEquals_DifferentOriginalLoanAmount() {
        // Arrange
        Money differentAmount = createMoney("12000.00", usdCurrency);
        LiabilityIncurrenceTransactionDetails details1 = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, interestPercentage, maturityDate
        );
        LiabilityIncurrenceTransactionDetails details2 = new LiabilityIncurrenceTransactionDetails(
            differentAmount, interestPercentage, maturityDate
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentInterestPercentage() {
        // Arrange
        Percentage differentInterest = createPercentage("0.06");
        LiabilityIncurrenceTransactionDetails details1 = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, interestPercentage, maturityDate
        );
        LiabilityIncurrenceTransactionDetails details2 = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, differentInterest, maturityDate
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentMaturityDate() {
        // Arrange
        Instant differentDate = maturityDate.plus(7, ChronoUnit.DAYS);
        LiabilityIncurrenceTransactionDetails details1 = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, interestPercentage, maturityDate
        );
        LiabilityIncurrenceTransactionDetails details2 = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, interestPercentage, differentDate
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_NullAndNonNullComparison() {
        // Arrange
        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, interestPercentage, maturityDate
        );

        // Assert
        assertFalse(details.equals(null));
    }

    @Test
    void testEquals_DifferentClass() {
        // Arrange
        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
            originalLoanAmount, interestPercentage, maturityDate
        );
        Object other = new Object(); // An object of a different class

        // Assert
        assertFalse(details.equals(other));
    }
}
