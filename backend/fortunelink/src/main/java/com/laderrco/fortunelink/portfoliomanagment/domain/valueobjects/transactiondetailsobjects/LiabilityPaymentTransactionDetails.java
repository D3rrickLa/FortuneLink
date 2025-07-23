package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.util.Objects;
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
        Objects.requireNonNull(liabilityId, "Liability id cannot be null.");
        Objects.requireNonNull(totalPaymentAmountInLiabilityCurrency, "Total payment amount in liability currency cannot be null.");
        Objects.requireNonNull(interestAmountInLiabilityCurrency, "Interest amount in liability currency cannot be null.");
        Objects.requireNonNull(feesAmountInLiabilityCurrency, "Fee amount in liability currency cannot be null.");
        Objects.requireNonNull(totalPaymentAmountInPortfolioCurrency, "Total payment amount in Portfolio currency cannot be null.");
        Objects.requireNonNull(interestAmountInPortfolioCurrency, "Interest amount in portfolio currency cannot be null.");
        Objects.requireNonNull(feesAmountInPortfolioCurrency, "Fee amount in portfolio currency cannot be null.");
        
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
