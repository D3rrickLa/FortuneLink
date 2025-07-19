package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import java.util.UUID;

import com.laderrco.fortunelink.shared.valueobjects.Money;

public final class LiabilityPaymentTransactionDetails extends TransactionDetails {
    private final UUID liabilityId;
    private final Money totalPaymentAmountInLiabilityCurrency;
    private final Money interestAmountInLiabilityCurrency;
    private final Money feesAmountInLiabilityCurrency;
    private final Money totalPaymentAmountInPortfolioCurrency;
    private final Money interestAmountInPortfolioCurrency;
    private final Money feesAmountInPortfolioCurrency;

    public LiabilityPaymentTransactionDetails(
        UUID liabilityId, 
        Money totalPaymentAmountInLiabilityCurrency,
        Money interestAmountInLiabilityCurrency, 
        Money feesAmountInLiabilityCurrency,
        Money totalPaymentAmountInPortfolioCurrency, 
        Money interestAmountInPortfolioCurrency,
        Money feesAmountInPortfolioCurrency
    ) {
        this.liabilityId = liabilityId;
        this.totalPaymentAmountInLiabilityCurrency = totalPaymentAmountInLiabilityCurrency; // principal + interest
        this.interestAmountInLiabilityCurrency = interestAmountInLiabilityCurrency; // amount from total that is interest
        this.feesAmountInLiabilityCurrency = feesAmountInLiabilityCurrency;
        this.totalPaymentAmountInPortfolioCurrency = totalPaymentAmountInPortfolioCurrency;
        this.interestAmountInPortfolioCurrency = interestAmountInPortfolioCurrency;
        this.feesAmountInPortfolioCurrency = feesAmountInPortfolioCurrency;
    }

    public UUID getLiabilityId() {
        return liabilityId;
    }
    public Money getTotalPaymentAmountInLiabilityCurrency() {
        return totalPaymentAmountInLiabilityCurrency;
    }
    public Money getInterestAmountInLiabilityCurrency() {
        return interestAmountInLiabilityCurrency;
    }
    public Money getFeesAmountInLiabilityCurrency() {
        return feesAmountInLiabilityCurrency;
    }
    public Money getTotalPaymentAmountInPortfolioCurrency() {
        return totalPaymentAmountInPortfolioCurrency;
    }
    public Money getInterestAmountInPortfolioCurrency() {
        return interestAmountInPortfolioCurrency;
    }
    public Money getFeesAmountInPortfolioCurrency() {
        return feesAmountInPortfolioCurrency;
    }
   
    
    
}
