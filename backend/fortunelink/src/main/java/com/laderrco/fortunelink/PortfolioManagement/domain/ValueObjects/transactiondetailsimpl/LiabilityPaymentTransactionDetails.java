package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

// Paying off a liability
public final class LiabilityPaymentTransactionDetails extends TransactionDetails {
    private final UUID liabilityId;
    private final Money totalOriginalPaymentAmount; // The total amount paid by the user in its original currency
    private final Money principalPaidAmount;        // Amount of originalPaymentAmount applied to principal (in liability's currency)
    private final Money interestPaidAmount;         // Amount of originalPaymentAmount applied to interest (in liability's currency)
    private final Money feesPaidAmount;             // Any direct fees associated with this payment (in liability's/portfolio's currency)
    private final Money cashOutflowInPortfolioCurrency; // The total cash removed from portfolio cash for this payment (in portfolio's currency, includes principal + interest + fees if applicable)

    public LiabilityPaymentTransactionDetails (
        UUID liabilityId,
        Money totalOriginalPaymentAmount,
        Money principalPaidAmount,
        Money interestPaidAmount,
        Money feesPaidAmount,
        Money cashOutflowInPortfolioCurrency
    ) {

        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
        Objects.requireNonNull(totalOriginalPaymentAmount, "Total original payment amount cannot be null.");
        Objects.requireNonNull(principalPaidAmount, "Principal paid amount cannot be null.");
        Objects.requireNonNull(interestPaidAmount, "Interest paid amount cannot be null.");
        Objects.requireNonNull(feesPaidAmount, "Fees paid amount cannot be null.");
        Objects.requireNonNull(cashOutflowInPortfolioCurrency, "Cash outflow in portfolio currency cannot be null.");
        
        // Validation for positive magnitudes where appropriate
        if (totalOriginalPaymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total original payment amount must be positive.");
        }
        if (principalPaidAmount.amount().compareTo(BigDecimal.ZERO) < 0) { // Can be zero for interest-only payment
            throw new IllegalArgumentException("Principal paid amount cannot be negative.");
        }
        if (interestPaidAmount.amount().compareTo(BigDecimal.ZERO) < 0) { // Can be zero if no interest or principal-only payment
            throw new IllegalArgumentException("Interest paid amount cannot be negative.");
        }
        if (feesPaidAmount.amount().compareTo(BigDecimal.ZERO) < 0) { // Can be zero if no fees
            throw new IllegalArgumentException("Fees paid amount cannot be negative.");
        }
        if (cashOutflowInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cash outflow in portfolio currency must be positive.");
        }
        
        // --- Consolidated Currency Consistency Check ---
        // Principal, interest, and fees breakdown amounts must be in the same currency as the total original payment amount.
        // This implicitly defines the "liability currency" for the payment breakdown.
        if (!principalPaidAmount.currency().equals(totalOriginalPaymentAmount.currency()) ||
            !interestPaidAmount.currency().equals(totalOriginalPaymentAmount.currency()) ||
            !feesPaidAmount.currency().equals(totalOriginalPaymentAmount.currency())) {
            throw new IllegalArgumentException("Principal, interest, and fees breakdown amounts must all be in the same currency as the total original payment amount (liability currency).");
        }
        // Note: cashOutflowInPortfolioCurrency's currency is independent and should be the portfolio's base currency.
        // No explicit cross-currency check here for it, as its currency is determined by the portfolio context.


        
        this.liabilityId = liabilityId;
        this.totalOriginalPaymentAmount = totalOriginalPaymentAmount;
        this.principalPaidAmount = principalPaidAmount;
        this.interestPaidAmount = interestPaidAmount;
        this.feesPaidAmount = feesPaidAmount;
        this.cashOutflowInPortfolioCurrency = cashOutflowInPortfolioCurrency;
    }

    public UUID getLiabilityId() {return liabilityId;}
    public Money getTotalOriginalPaymentAmount() {return totalOriginalPaymentAmount;}
    public Money getPrincipalPaidAmount() {return principalPaidAmount;}
    public Money getInterestPaidAmount() {return interestPaidAmount;}
    public Money getFeesPaidAmount() {return feesPaidAmount;}
    public Money getCashOutflowInPortfolioCurrency() {return cashOutflowInPortfolioCurrency;}

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        LiabilityPaymentTransactionDetails that = (LiabilityPaymentTransactionDetails) o;
        return Objects.equals(this.liabilityId, that.liabilityId)
                && Objects.equals(this.totalOriginalPaymentAmount, that.totalOriginalPaymentAmount)
                && Objects.equals(this.principalPaidAmount, that.principalPaidAmount)
                && Objects.equals(this.interestPaidAmount, that.interestPaidAmount)
                && Objects.equals(this.feesPaidAmount, that.feesPaidAmount)
                && Objects.equals(this.cashOutflowInPortfolioCurrency, that.cashOutflowInPortfolioCurrency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.liabilityId, this.totalOriginalPaymentAmount, this.principalPaidAmount, this.interestPaidAmount, this.feesPaidAmount, this.cashOutflowInPortfolioCurrency);
    }
}