package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionType;

public class ReversalTransactionDetailsTest {
       // Common test data
    private UUID originalTransactionId;
    private TransactionType transactionType;
    private String reasonForReversal;

    @BeforeEach
    void init() {
        originalTransactionId = UUID.randomUUID();
        transactionType = TransactionType.OTHER; // Example type
        reasonForReversal = "Error in original cash inflow recording.";
    }


    @Test
    void testConstructor_ValidDetails() {
        // Act
        ReversalTransactionDetails details = new ReversalTransactionDetails(
            originalTransactionId, reasonForReversal, transactionType
        );

        // Assert
        assertNotNull(details);
        assertEquals(originalTransactionId, details.getOriginalTransactionId());
        assertEquals(transactionType, details.getTransactionType());
        assertEquals(reasonForReversal, details.getReasonForReversal());
    }

    @Test
    void testConstructor_ValidDetails_DifferentTransactionType() {
        // Arrange
        TransactionType differentType = TransactionType.PAYMENT;
        String newReason = "Payment was duplicated.";

        // Act
        ReversalTransactionDetails details = new ReversalTransactionDetails(
            UUID.randomUUID(), newReason, differentType
        );

        // Assert
        assertNotNull(details);
        assertEquals(newReason, details.getReasonForReversal());
        assertEquals(differentType, details.getTransactionType());
    }

    // --- Test Constructor - Unhappy Paths (Validation) ---

    @Test
    void testConstructor_ThrowsExceptionForNullOriginalTransactionId() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new ReversalTransactionDetails(null, reasonForReversal, transactionType);
        });
        assertTrue(thrown.getMessage().contains("Original Transaction ID for reversal cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullTransactionType() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new ReversalTransactionDetails(originalTransactionId, reasonForReversal, null);
        });
        assertTrue(thrown.getMessage().contains("Transaction Type for reversal cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullReasonForReversal() {
        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new ReversalTransactionDetails(originalTransactionId, null, transactionType);
        });
        assertTrue(thrown.getMessage().contains("Reason For Reversal cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForEmptyReasonForReversal() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new ReversalTransactionDetails(originalTransactionId, "", transactionType);
        });
        assertTrue(thrown.getMessage().contains("Reason for reversal cannot be blank."));
    }

    @Test
    void testConstructor_ThrowsExceptionForBlankReasonForReversal() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new ReversalTransactionDetails(originalTransactionId, "   \t\n ", transactionType); // Contains only whitespace
        });
        assertTrue(thrown.getMessage().contains("Reason for reversal cannot be blank."));
    }

    // --- Test Equals and HashCode ---

    @Test
    void testEquals_SameObjects() {
        // Arrange
        ReversalTransactionDetails details1 = new ReversalTransactionDetails(
            originalTransactionId, reasonForReversal, transactionType
        );
        ReversalTransactionDetails details2 = new ReversalTransactionDetails(
            originalTransactionId, reasonForReversal, transactionType
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
    void testEquals_DifferentOriginalTransactionId() {
        // Arrange
        ReversalTransactionDetails details1 = new ReversalTransactionDetails(
            originalTransactionId, reasonForReversal, transactionType
        );
        ReversalTransactionDetails details2 = new ReversalTransactionDetails(
            UUID.randomUUID(), reasonForReversal, transactionType
        ); // Different ID

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentTransactionType() {
        // Arrange
        ReversalTransactionDetails details1 = new ReversalTransactionDetails(
            originalTransactionId, reasonForReversal, TransactionType.OTHER
        );
        ReversalTransactionDetails details2 = new ReversalTransactionDetails(
            originalTransactionId, reasonForReversal, TransactionType.CORPORATE_ACTION
        ); // Different type

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentReasonForReversal() {
        // Arrange
        ReversalTransactionDetails details1 = new ReversalTransactionDetails(
            originalTransactionId, "Reason A", transactionType
        );
        ReversalTransactionDetails details2 = new ReversalTransactionDetails(
            originalTransactionId, "Reason B", transactionType
        ); // Different reason

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_NullAndNonNullComparison() {
        // Arrange
        ReversalTransactionDetails details = new ReversalTransactionDetails(
            originalTransactionId, reasonForReversal, transactionType
        );

        // Assert
        assertFalse(details.equals(null));
    }

    @Test
    void testEquals_DifferentClass() {
        // Arrange
        ReversalTransactionDetails details = new ReversalTransactionDetails(
            originalTransactionId, reasonForReversal, transactionType
        );
        Object other = new Object(); // An object of a different class

        // Assert
        assertFalse(details.equals(other));
    }
}
