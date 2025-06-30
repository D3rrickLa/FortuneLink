package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public class CashTransactionDetails extends TransactionDetails {
    // Intended to be empty, all the fields needed are in the Transaction Class
    // In the future we could have custom tags for 'salary deposit', etc. then we
    // add stuff here
    // the description from Transaction itself is good enough
    private final Money normalizedAmount;

    public CashTransactionDetails(Money normalizedAmount) {
        Objects.requireNonNull(normalizedAmount, "Amount cannot be null.");
        if (normalizedAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount cannot be less than zero.");
        }
        this.normalizedAmount = normalizedAmount;
    }

    
    public Money getNormalizedAmount() {
        return normalizedAmount;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CashTransactionDetails that = (CashTransactionDetails) o;
        return Objects.equals(this.normalizedAmount, that.normalizedAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.normalizedAmount);
    }



}
