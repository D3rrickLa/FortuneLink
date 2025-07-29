package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public final class InterestExpenseDetails extends TransactionDetails {
    private final UUID liabilityId;
    private final Money amountAccruedOrPaid;
    public InterestExpenseDetails(UUID liabilityId, Money amountAccruedOrPaid) {
        this.liabilityId = liabilityId;
        this.amountAccruedOrPaid = amountAccruedOrPaid;
    }
    public UUID getLiabilityId() {
        return liabilityId;
    }
    public Money getAmountAccruedOrPaid() {
        return amountAccruedOrPaid;
    }

    

}
