package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;

public final class LiabilityIncurrenceTransactionDetails extends TransactionDetails {
    // might need a liability identifier class
    private final LiabilityId liabilityId;
    private final Money principalAmount;
    private final Percentage interestRate; // year

    protected LiabilityIncurrenceTransactionDetails(
        LiabilityId liabilityId,
        Money principalAmount,
        Percentage interestRate,

        TransactionSource source, 
        String description, 
        List<Fee> fees
    ) {
        super(source, description, fees);

        validateParameter(liabilityId, "Liabilty id");
        validateParameter(principalAmount, "Principal amount");
        validateParameter(interestRate, "Interest rate");

        this.liabilityId = liabilityId;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }

    public LiabilityId getLiabilityId() {
        return liabilityId;
    }

    public Money getPrincipalAmount() {
        return principalAmount;
    }

    public Percentage getInterestRate() {
        return interestRate;
    }
    
}
