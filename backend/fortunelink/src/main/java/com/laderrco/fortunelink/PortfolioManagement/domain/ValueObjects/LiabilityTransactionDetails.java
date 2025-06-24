package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

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
    private final Money principalChange; // how much principal was affected by this payment/loan

    public LiabilityTransactionDetails(UUID liabilityId, Money interestPaid, Money principalChange) {
        this.liabilityId = liabilityId;
        this.interestPaid = interestPaid;
        this.principalChange = principalChange;
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

    
}
