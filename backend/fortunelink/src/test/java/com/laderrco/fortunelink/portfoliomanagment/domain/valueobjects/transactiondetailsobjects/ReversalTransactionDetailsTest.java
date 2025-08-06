package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.TransactionId;

public class ReversalTransactionDetailsTest {
    @Test
    public void testConstructorAndGetters() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        String reason = "Duplicate transaction";
        TransactionSource source = TransactionSource.MANUAL;
        String description = "Reversal due to duplication";
        List<Fee> fees = List.of(new Fee(FeeType.REGULATORY, new Money(BigDecimal.valueOf(5.00), Currency.getInstance("USD")), "Processing"));

        // Act
        ReversalTransactionDetails details = new ReversalTransactionDetails(
            new TransactionId(transactionId),
            reason,
            source,
            description,
            fees
        );

        // Assert
        assertEquals(transactionId, details.getTransactionId().transactionId());
        assertEquals(reason, details.getReason());
        assertEquals(source, details.getSource());
        assertEquals(description, details.getDescription());
        assertEquals(fees, details.getFees());
    }

    @Test
    public void testNullReasonAllowed() {
        UUID transactionId = UUID.randomUUID();
        ReversalTransactionDetails details = new ReversalTransactionDetails(
            new TransactionId(transactionId),
            null,
            TransactionSource.SYSTEM,
            "Auto reversal",
            List.of()
        );

        assertNull(details.getReason());
    }

    @Test
    public void testEmptyFeesList() {
        ReversalTransactionDetails details = new ReversalTransactionDetails(
            new TransactionId(UUID.randomUUID()),
            "No fees involved",
            TransactionSource.SYSTEM,
            "Reversal without fees",
            List.of()
        );

        assertTrue(details.getFees().isEmpty());
    }
}
