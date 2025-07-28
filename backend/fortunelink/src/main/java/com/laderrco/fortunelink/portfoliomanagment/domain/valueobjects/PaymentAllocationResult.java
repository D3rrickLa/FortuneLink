package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.util.Objects;

public record PaymentAllocationResult(Money principalPaid, Money interestPaid, Money remainingBalance) {
    public PaymentAllocationResult {
        Objects.requireNonNull(principalPaid, "Principal paid cannot be null.");
        Objects.requireNonNull(interestPaid, "Interest paid cannot be null.");
        Objects.requireNonNull(remainingBalance, "Remaining balance cannot be null.");

        if (!principalPaid.currency().equals(interestPaid.currency()) || !principalPaid.currency().equals(remainingBalance.currency())) {
            throw new IllegalArgumentException("All amounts in PaymentAllocationResult must have the same currency.");
        }
    }
}