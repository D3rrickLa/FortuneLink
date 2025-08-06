package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;

public class LiabilityPaymentTransactionDetailsTest {
    private final LiabilityId validLiabilityId = new LiabilityId(UUID.randomUUID());
    private final Money validPrincipal = new Money(1000.00, "USD");
    private final Money validInterest = new Money(50.00, "USD");
    private final TransactionSource validSource = TransactionSource.MANUAL;
    private final String validDescription = "Loan repayment";
    private final List<Fee> validFees = List.of(new Fee(FeeType.BROKERAGE, new Money(10.00, "USD"), "Processing"));

    @Test
    void constructor_shouldCreateObject_whenAllParametersAreValid() {
        LiabilityPaymentTransactionDetails details = new LiabilityPaymentTransactionDetails(
            validLiabilityId,
            validPrincipal,
            validInterest,
            validSource,
            validDescription,
            validFees
        );

        assertEquals(validLiabilityId, details.getLiabilityId());
        assertEquals(validPrincipal, details.getPrincipalPaymentAmount());
        assertEquals(validInterest, details.getInterestPaymentAmount());
        assertEquals(validSource, details.getSource());
        assertEquals(validDescription, details.getDescription());
        assertEquals(validFees, details.getFees());
    }

    @Test
    void constructor_shouldThrowException_whenLiabilityIdIsNull() {
        Executable executable = () -> new LiabilityPaymentTransactionDetails(
            null,
            validPrincipal,
            validInterest,
            validSource,
            validDescription,
            validFees
        );

        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("Liabilty id cannot be null.", exception.getMessage());
    }

    @Test
    void constructor_shouldThrowException_whenPrincipalPaymentAmountIsNull() {
        Executable executable = () -> new LiabilityPaymentTransactionDetails(
            validLiabilityId,
            null,
            validInterest,
            validSource,
            validDescription,
            validFees
        );

        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("Principal payment amount cannot be null.", exception.getMessage());
    }

    @Test
    void constructor_shouldThrowException_whenInterestPaymentAmountIsNull() {
        Executable executable = () -> new LiabilityPaymentTransactionDetails(
            validLiabilityId,
            validPrincipal,
            null,
            validSource,
            validDescription,
            validFees
        );

        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("Interest payment amount cannot be null.", exception.getMessage());
    }
}
