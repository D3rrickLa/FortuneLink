package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Percentage;

public class LiabilityIncurrenceTransactionDetails extends TransactionDetails {
    private final Money originalLoanAmount;
    private final Percentage interestPercentage;
    private final Instant maturityDate;
    public LiabilityIncurrenceTransactionDetails(Money originalLoanAmount, Percentage interestPercentage, Instant maturityDate) {
        Objects.requireNonNull(originalLoanAmount, "originalLoanAmount cannot be null.");
        Objects.requireNonNull(interestPercentage, "interestPercentage cannot be null.");
        Objects.requireNonNull(maturityDate, "maturityDate cannot be null.");

        if (originalLoanAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Liability iniital amount must be greater than zero.");
        }

        this.originalLoanAmount = originalLoanAmount;
        this.interestPercentage = interestPercentage;
        this.maturityDate = maturityDate;
    }
    
    public Money getOriginalLoanAmount() {
        return originalLoanAmount;
    }
    public Percentage getInterestPercentage() {
        return interestPercentage;
    }
    public Instant getMaturityDate() {
        return maturityDate;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        
        LiabilityIncurrenceTransactionDetails that = (LiabilityIncurrenceTransactionDetails) o;
        return Objects.equals(this.originalLoanAmount, that.originalLoanAmount) 
        && Objects.equals(this.interestPercentage, that.interestPercentage)
        && Objects.equals(this.maturityDate, that.maturityDate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.originalLoanAmount, this.interestPercentage, this.maturityDate);
    }
    
}
