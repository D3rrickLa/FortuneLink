package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

// the reason why we used id here because the loan is guarenteed to exist already
// we don't just pay for a car payment, we do a specific one
// When you buy an asset, you're acquiring a new portion of a type of asset. 
// You don't necessarily have an AssetHolding ID for it yet, or you're adding to an existing one. 
// The transaction identifies the type (AssetIdentifier) you're dealing with. 
// When you interact with a liability (make a payment, or receive the loan funds), 
// you are always interacting with a pre-existing, specific debt instance (identified by its liabilityId),
// or the transaction itself creates that specific debt instance which then gets an ID.
public class LiabilityTransactionDetails extends TransactionDetails {
    private final UUID liabilityId;
    private final Money interestPaid;
    private final Money principalChange; // how much principal was affected by this payment

    public LiabilityTransactionDetails(UUID liabilityId, Money interestPaid, Money principalChange) {
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
        Objects.requireNonNull(interestPaid, "Interest paid cannot be null..");
        Objects.requireNonNull(principalChange, "Principal cannot be null.");

        if (interestPaid.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount paid going towards Interest cannot be negative.");
        }

        if (principalChange.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount paid going towards Principal cannot be negative.");
        }

        this.liabilityId = liabilityId;
        this.interestPaid = interestPaid;
        this.principalChange = principalChange; // how much went to the initial amount
    }

    public UUID getLiabilityId() {
        return liabilityId;
    }

    public Money getInterestPaid() {
        return interestPaid;
    }

    public Money getPrincipalChange() {
        return principalChange;
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
                && Objects.equals(this.interestPaid, that.interestPaid)
                && Objects.equals(this.principalChange, that.principalChange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.liabilityId, this.interestPaid, this.principalChange);
    }

}
