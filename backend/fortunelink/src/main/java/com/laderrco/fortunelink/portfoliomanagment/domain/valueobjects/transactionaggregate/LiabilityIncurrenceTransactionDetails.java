package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import java.time.Instant;

import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public final class LiabilityIncurrenceTransactionDetails extends TransactionDetails {
    private final Money originalLoanAmount;
    private final Percentage annualInterestRate;
    private final Instant maturityDate;
    
    public LiabilityIncurrenceTransactionDetails(Money originalLoanAmount, Percentage annualInterestRate, Instant maturityDate) {
        this.originalLoanAmount = originalLoanAmount;
        this.annualInterestRate = annualInterestRate;
        this.maturityDate = maturityDate;
    }
    public Money getOriginalLoanAmount() {
        return originalLoanAmount;
    }
    public Percentage getAnnualInterestRate() {
        return annualInterestRate;
    }
    public Instant getMaturityDate() {
        return maturityDate;
    }

}
