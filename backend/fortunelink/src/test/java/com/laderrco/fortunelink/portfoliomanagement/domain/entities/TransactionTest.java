package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl.CashflowTransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class TransactionTest {
    private UUID transactionId;
    private UUID portfolioId;
    private TransactionType transactionType;
    private Money totalTransactionAmount;
    private Instant transactionDate;
    private TransactionDetails transactionDetails;
    private TransactionMetadata transactionMetadata;
    private List<Fee> fees;
    private boolean hidden;
    private PortfolioCurrency usd;

    @BeforeEach
    void init() {
        transactionId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        usd = new PortfolioCurrency(Currency.getInstance("USD"));

        transactionType = TransactionType.DEPOSIT;
        totalTransactionAmount = new Money(
            new BigDecimal(2200.38), 
            usd);
        transactionDate = Instant.now();
        transactionMetadata = new TransactionMetadata (
            TransactionStatus.COMPLETED, 
            TransactionSource.MANUAL_INPUT, 
            "Some description.", 
            transactionDate, 
            transactionDate
        );

        fees = new ArrayList<>();
        fees.add(
            new Fee(FeeType.DEPOSIT_FEE, new Money(new BigDecimal(2), usd))
            );
            fees.add(
            new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money(new BigDecimal(2), usd))
        );


        transactionDetails = new CashflowTransactionDetails(
            totalTransactionAmount, 
            totalTransactionAmount.multiply(new BigDecimal(0.72)), 
            new BigDecimal(1.38), 
            new Money(new BigDecimal(2), usd), 
            new Money(new BigDecimal(2), usd)
        );

        hidden = false;
    }

    @Test
    void testConstructor_ValidArguments_AllFieldsProvided() {
        // Act
        Transaction transaction = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );

        // Assert
        assertNotNull(transaction);
        assertEquals(transactionId, transaction.getTransactionId());
        assertEquals(portfolioId, transaction.getPortfolioId());
        assertEquals(transactionType, transaction.getTransactionType());
        assertEquals(totalTransactionAmount, transaction.getTotalTransactionAmount());
        assertEquals(transactionDate, transaction.getTransactionDate());
        assertEquals(transactionDetails, transaction.getTransactionDetails());
        assertEquals(transactionMetadata, transaction.getTransactionMetadata());
        assertEquals(fees, transaction.getFees()); // Verifies defensive copy
        assertEquals(hidden, transaction.isHidden());
    }
    
    @Test
    void testConstructor_ValidArguments_EmptyFeesList() {
        // Arrange
        List<Fee> emptyFees = new ArrayList<>();

        // Act
        Transaction transaction = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, emptyFees, hidden
        );

        // Assert
        assertNotNull(transaction);
        assertTrue(transaction.getFees().isEmpty());
    }

// --- Constructor Unhappy Path Tests (Null Checks) ---

    @Test
    void testConstructor_ThrowsNullPointerException_TransactionIdNull() {
        assertThrows(NullPointerException.class, () -> new Transaction(
            null, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        ), "Transaction ID cannot be null.");
    }

    @Test
    void testConstructor_ThrowsNullPointerException_PortfolioIdNull() {
        assertThrows(NullPointerException.class, () -> new Transaction(
            transactionId, null, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        ), "Portfolio ID cannot be null.");
    }

    @Test
    void testConstructor_ThrowsNullPointerException_TransactionTypeNull() {
        assertThrows(NullPointerException.class, () -> new Transaction(
            transactionId, portfolioId, null, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        ), "Transaction Type cannot be null.");
    }

    @Test
    void testConstructor_ThrowsNullPointerException_TotalTransactionAmountNull() {
        assertThrows(NullPointerException.class, () -> new Transaction(
            transactionId, portfolioId, transactionType, null,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        ), "Total Transaction Amount cannot be null.");
    }

    @Test
    void testConstructor_ThrowsNullPointerException_TransactionDateNull() {
        assertThrows(NullPointerException.class, () -> new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            null, transactionDetails, transactionMetadata, fees, hidden
        ), "Transaction Date cannot be null.");
    }

    @Test
    void testConstructor_ThrowsNullPointerException_TransactionDetailsNull() {
        assertThrows(NullPointerException.class, () -> new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, null, transactionMetadata, fees, hidden
        ), "Transaction Details cannot be null.");
    }

    @Test
    void testConstructor_ThrowsNullPointerException_TransactionMetadataNull() {
        assertThrows(NullPointerException.class, () -> new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, null, fees, hidden
        ), "Transaction Metadata cannot be null.");
    }

    @Test
    void testConstructor_ThrowsNullPointerException_FeesNull() {
        assertThrows(NullPointerException.class, () -> new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, null, hidden
        ), "Fees list cannot be null.");
    }

    // --- setHidden() Tests ---

    @Test
    void testSetHidden_FromFalseToTrue() {
        // Arrange
        Transaction transaction = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, false
        );
        assertFalse(transaction.isHidden());

        // Act
        transaction.setHidden(true);

        // Assert
        assertTrue(transaction.isHidden());
    }

    @Test
    void testSetHidden_FromTrueToFalse() {
        // Arrange
        Transaction transaction = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, true
        );
        assertTrue(transaction.isHidden());

        // Act
        transaction.setHidden(false);

        // Assert
        assertFalse(transaction.isHidden());
    }

    @Test
    void testSetHidden_NoChange() {
        // Arrange
        Transaction transaction = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, false
        );
        assertFalse(transaction.isHidden());

        // Act
        transaction.setHidden(false); // Set to same value

        // Assert
        assertFalse(transaction.isHidden()); // Still false
    }

    // --- equals() and hashCode() Tests ---

    @Test
    void testEquals_SameObject() {
        Transaction transaction = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        assertTrue(transaction.equals(transaction));
        assertEquals(transaction.hashCode(), transaction.hashCode());
    }

    @Test
    void testEquals_EqualObjects() {
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Transaction transaction2 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        assertTrue(transaction1.equals(transaction2));
        assertEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_DifferentTransactionId() {
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Transaction transaction2 = new Transaction(
            UUID.randomUUID(), portfolioId, transactionType, totalTransactionAmount, // Different ID
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        assertFalse(transaction1.equals(transaction2));
        assertNotEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_DifferentPortfolioId() {
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Transaction transaction2 = new Transaction(
            transactionId, UUID.randomUUID(), transactionType, totalTransactionAmount, // Different Portfolio ID
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        assertFalse(transaction1.equals(transaction2));
        assertNotEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_DifferentTransactionType() {
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, TransactionType.BUY, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Transaction transaction2 = new Transaction(
            transactionId, portfolioId, TransactionType.SELL, totalTransactionAmount, // Different Type
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        assertFalse(transaction1.equals(transaction2));
        assertNotEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_DifferentTotalTransactionAmount() {
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Transaction transaction2 = new Transaction(
            transactionId, portfolioId, transactionType, new Money(new BigDecimal("1200.00"), usd), // Different Amount
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        assertFalse(transaction1.equals(transaction2));
        assertNotEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_DifferentTransactionDate() {
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Transaction transaction2 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            Instant.now().plusSeconds(1), transactionDetails, transactionMetadata, fees, hidden // Different Date
        );
        assertFalse(transaction1.equals(transaction2));
        assertNotEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_DifferentTransactionDetails() {
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Transaction transaction2 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, new CashflowTransactionDetails(
                totalTransactionAmount, 
                totalTransactionAmount.multiply(new BigDecimal(0.72)), 
                new BigDecimal(1.38), 
                new Money(new BigDecimal(4), usd), 
                new Money(new BigDecimal(2), usd)
        ), transactionMetadata, fees, hidden // Different Details
        );
        assertFalse(transaction1.equals(transaction2));
        assertNotEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_DifferentTransactionMetadata() {
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Transaction transaction2 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails,   new TransactionMetadata (
            TransactionStatus.COMPLETED, 
            TransactionSource.PLATFORM_SYNC, 
            "Some description 2.", 
            transactionDate, 
            transactionDate
        ), fees, hidden // Different Metadata
        );
        assertFalse(transaction1.equals(transaction2));
        assertNotEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_DifferentFeesList() {
        // Arrange
        List<Fee> differentFees = new ArrayList<>();
        differentFees.add(
            new Fee(FeeType.DEPOSIT_FEE, new Money(new BigDecimal(20), usd))
            );
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Transaction transaction2 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, differentFees, hidden // Different Fees
        );
        assertFalse(transaction1.equals(transaction2));
        assertNotEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_DifferentHiddenStatus() {
        Transaction transaction1 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, false
        );
        Transaction transaction2 = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, true // Different hidden status
        );
        assertFalse(transaction1.equals(transaction2));
        assertNotEquals(transaction1.hashCode(), transaction2.hashCode());
    }

    @Test
    void testEquals_NullObject() {
        Transaction transaction = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        assertFalse(transaction.equals(null));
    }

    @Test
    void testEquals_DifferentClass() {
        Transaction transaction = new Transaction(
            transactionId, portfolioId, transactionType, totalTransactionAmount,
            transactionDate, transactionDetails, transactionMetadata, fees, hidden
        );
        Object obj = new Object();
        assertFalse(transaction.equals(obj));
    }

}
