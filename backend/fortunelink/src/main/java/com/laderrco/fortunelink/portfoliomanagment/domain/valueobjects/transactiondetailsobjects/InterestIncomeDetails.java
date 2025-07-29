package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public final class InterestIncomeDetails extends TransactionDetails {
    private final Money amountEarned;
    private final String sourceDescription;
    private final UUID realtedAccountId;
    
    public InterestIncomeDetails(Money amountEarned, String sourceDescription, UUID realtedAccountId) {
        this.amountEarned = amountEarned;
        this.sourceDescription = sourceDescription;
        this.realtedAccountId = realtedAccountId;
    }

    public Money getAmountEarned() {
        return amountEarned;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public UUID getRealtedAccountId() {
        return realtedAccountId;
    }

        
}
