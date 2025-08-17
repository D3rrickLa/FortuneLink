package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.util.Objects;

// used as a DTO almost, summarizing what you paid and what is left
public record PaymentAllocationResult(
    Money principalPaid,
    Money interestPaid,
    Money remainingBalance,
    Money overPayment
) {
    public PaymentAllocationResult {
        Objects.requireNonNull(principalPaid, "Principal amount cannot be null.");
        Objects.requireNonNull(interestPaid, "Interest amount cannot be null.");
        Objects.requireNonNull(remainingBalance, "Remaining balance cannot be null.");
        Objects.requireNonNull(overPayment, "Overpayment cannot be null.");

        if (!principalPaid.currency().equals(interestPaid.currency()) || !principalPaid.currency().equals(remainingBalance.currency())) {
            throw new IllegalArgumentException("All amounts in PaymentAllocationResult must have the same currency.");
        }
    }
}
