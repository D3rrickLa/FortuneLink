package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;

// liabilities should be in Portoflio Currency pref
public final class LiabilityPaymentTransactionDetails extends TransactionDetails {
    private final LiabilityId liabilityId;
    private final Money principalPaymentAmount;
    private final Money interestPaymentAmount;
    public LiabilityPaymentTransactionDetails(
        LiabilityId liabilityId, 
        Money principalPaymentAmount, 
        Money interestPaymentAmount,

        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        super(source, description, fees);
        
        validateParameter(liabilityId, "Liabilty id");
        validateParameter(principalPaymentAmount, "Principal payment amount");
        validateParameter(interestPaymentAmount, "Interest payment amount");

        this.liabilityId = liabilityId;
        this.principalPaymentAmount = principalPaymentAmount;
        this.interestPaymentAmount = interestPaymentAmount;
    }    
    
    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }
    
    public LiabilityId getLiabilityId() {
        return liabilityId;
    }
    
    public Money getPrincipalPaymentAmount() {
        return principalPaymentAmount;
    }

    public Money getInterestPaymentAmount() {
        return interestPaymentAmount;
    }    
}
