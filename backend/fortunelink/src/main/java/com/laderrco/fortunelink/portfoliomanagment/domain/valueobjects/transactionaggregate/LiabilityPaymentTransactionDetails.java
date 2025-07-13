package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import java.util.UUID;

import com.laderrco.fortunelink.shared.valueobjects.Money;

public final class LiabilityPaymentTransactionDetails extends TransactionDetails {
    private final UUID liabilityId;
    private final Money totalPaymentAmount;
    private final Money interestAmount;
    private final Money feesAmount;
   
    public LiabilityPaymentTransactionDetails(
        UUID liabilityId, 
        Money totalPaymentAmount, 
        Money interestAmount,
        Money feesAmount
    ) {
        this.liabilityId = liabilityId;
        this.totalPaymentAmount = totalPaymentAmount;
        this.interestAmount = interestAmount;
        this.feesAmount = feesAmount;
    }
    
    public UUID getLiabilityId() {return liabilityId;}
    public Money getTotalPaymentAmount() {return totalPaymentAmount;}
    public Money getInterestAmount() {return interestAmount;}
    public Money getFeesAmount() {return feesAmount;}
    
    
}
