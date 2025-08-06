package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;

public class CashflowTransactionDetailsTest {
    @Test
    public void testValidConstructionAndGetters() {
        // Arrange
        Money amount = new Money(100.00, "CAD");
        CashflowType type = CashflowType.DEPOSIT;
        TransactionSource source = TransactionSource.MANUAL;
        String description = "Initial deposit";
        List<Fee> fees = List.of(new Fee(FeeType.OTHER, new Money(2.00, "CAD"), "Service Fee"));

        // Act
        CashflowTransactionDetails details = new CashflowTransactionDetails(
            amount,
            type,
            source,
            description,
            fees
        );

        // Assert
        assertEquals(amount, details.getAmount());
        assertEquals(type, details.getCashflowType());
        assertEquals(source, details.getSource());
        assertEquals(description, details.getDescription());
        assertEquals(fees, details.getFees());
    }

    @Test
    public void testNullAmountThrowsException() {
        CashflowType type = CashflowType.WITHDRAWAL;
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new CashflowTransactionDetails(
                null,
                type,
                TransactionSource.SYSTEM,
                "Withdrawal",
                List.of()
            );
        });

        assertEquals("Amount cannot be null.", exception.getMessage());
    }

    @Test
    public void testNullCashflowTypeThrowsException() {
        Money amount = new Money(50.00, "CAD");
        Exception exception = assertThrows(NullPointerException.class, () -> {
            new CashflowTransactionDetails(
                amount,
                null,
                TransactionSource.SYSTEM,
                "Deposit",
                List.of()
            );
        });

        assertEquals("Cashflow type cannot be null.", exception.getMessage());
    }

    @Test
    public void testEmptyFeesListIsAllowed() {
        Money amount = new Money(75.00, "CAD");
        CashflowType type = CashflowType.DEPOSIT;

        CashflowTransactionDetails details = new CashflowTransactionDetails(
            amount,
            type,
            TransactionSource.SYSTEM,
            "Deposit without fees",
            List.of()
        );

        assertTrue(details.getFees().isEmpty());
    }
}
