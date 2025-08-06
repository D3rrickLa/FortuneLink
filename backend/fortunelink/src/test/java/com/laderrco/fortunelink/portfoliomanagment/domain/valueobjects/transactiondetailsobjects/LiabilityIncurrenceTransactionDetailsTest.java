package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;

public class LiabilityIncurrenceTransactionDetailsTest {
    private final LiabilityId validLiabilityId = new LiabilityId(UUID.randomUUID());
    private final Money validPrincipal = new Money(5000.00, "USD");
    private final Percentage validInterestRate = new Percentage(BigDecimal.valueOf(5.0)); // Assuming 5% annual
    private final TransactionSource validSource = TransactionSource.SYSTEM;
    private final String validDescription = "New liability incurred";
    private final List<Fee> validFees = List.of(new Fee(FeeType.OTHER, new Money(100.00, "USD"), "Origination"));

    @Test
    void constructor_shouldCreateObject_whenAllParametersAreValid() {
        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
            validLiabilityId,
            validPrincipal,
            validInterestRate,
            validSource,
            validDescription,
            validFees
        );

        assertEquals(validLiabilityId, details.getLiabilityId());
        assertEquals(validPrincipal, details.getPrincipalAmount());
        assertEquals(validInterestRate, details.getInterestRate());
        assertEquals(validSource, details.getSource());
        assertEquals(validDescription, details.getDescription());
        assertEquals(validFees, details.getFees());
    }

    @Test
    void constructor_shouldThrowException_whenLiabilityIdIsNull() {
        Executable executable = () -> new LiabilityIncurrenceTransactionDetails(
            null,
            validPrincipal,
            validInterestRate,
            validSource,
            validDescription,
            validFees
        );

        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("Liabilty id cannot be null.", exception.getMessage());
    }

    @Test
    void constructor_shouldThrowException_whenPrincipalAmountIsNull() {
        Executable executable = () -> new LiabilityIncurrenceTransactionDetails(
            validLiabilityId,
            null,
            validInterestRate,
            validSource,
            validDescription,
            validFees
        );

        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("Principal amount cannot be null.", exception.getMessage());
    }

    @Test
    void constructor_shouldThrowException_whenInterestRateIsNull() {
        Executable executable = () -> new LiabilityIncurrenceTransactionDetails(
            validLiabilityId,
            validPrincipal,
            null,
            validSource,
            validDescription,
            validFees
        );

        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("Interest rate cannot be null.", exception.getMessage());
    }

}
