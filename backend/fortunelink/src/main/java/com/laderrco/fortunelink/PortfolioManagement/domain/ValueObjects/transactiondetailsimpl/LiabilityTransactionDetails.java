package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

// this is for establishing the liability
public final class LiabilityTransactionDetails extends TransactionDetails {
    private final UUID liabilityId;
    private final Money initialInterest;
    private final Money principalAmount; // how much principal was affected by this payment
 
    public LiabilityTransactionDetails(UUID liabilityId, Money initialInterest, Money principalAmount) {
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
        Objects.requireNonNull(initialInterest, "Interest cannot be null.");
        Objects.requireNonNull(principalAmount, "Principal cannot be null.");

        if (initialInterest.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount paid going towards Interest cannot be negative.");
        }

        if (principalAmount.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount paid going towards Principal cannot be negative.");
        }

        this.liabilityId = liabilityId;
        this.initialInterest = initialInterest;
        this.principalAmount = principalAmount; // how much went to the initial amount
    }

    public UUID getLiabilityId() {
        return liabilityId;
    }

    public Money getInterestPaid() {
        return initialInterest;
    }

    public Money getPrincipalChange() {
        return principalAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LiabilityTransactionDetails that = (LiabilityTransactionDetails) o;
        return Objects.equals(this.liabilityId, that.liabilityId)
                && Objects.equals(this.initialInterest, that.initialInterest)
                && Objects.equals(this.principalAmount, that.principalAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.liabilityId, this.initialInterest, this.principalAmount);
    }
}