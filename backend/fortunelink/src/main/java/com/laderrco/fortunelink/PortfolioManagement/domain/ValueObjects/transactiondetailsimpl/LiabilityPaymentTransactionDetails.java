package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

// Paying off a liability
public final class LiabilityPaymentTransactionDetails extends TransactionDetails {
    private final UUID liabilityId;
    private final Money originalPaymentAmount; // The amount paid in its original currency (e.g., USD)
    private final Money amountAppliedToLiability; // The amount that reduced the liability's balance (in liability's currency)
    private final Money cashOutflowInPortfolioCurrency; // The total cash removed from portfolio cash for this payment (in portfolio's currency, includes principal + direct fees if any)

    public LiabilityPaymentTransactionDetails(UUID liabilityId, Money originalPaymentAmount, Money amountAppliedToLiability, Money cashOutflowInPortfolioCurrency) {
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
        Objects.requireNonNull(originalPaymentAmount, "Original payment amount cannot be null.");
        Objects.requireNonNull(amountAppliedToLiability, "Amount applied to liability cannot be null.");
        Objects.requireNonNull(cashOutflowInPortfolioCurrency, "Cash outflow in portfolio currency cannot be null.");

        // Add any necessary value validations here (e.g., amounts must be positive magnitudes)
        if (originalPaymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0 ||
            amountAppliedToLiability.amount().compareTo(BigDecimal.ZERO) <= 0 ||
            cashOutflowInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amounts must be positive magnitudes.");
        }


        this.liabilityId = liabilityId;
        this.originalPaymentAmount = originalPaymentAmount;
        this.amountAppliedToLiability = amountAppliedToLiability;
        this.cashOutflowInPortfolioCurrency = cashOutflowInPortfolioCurrency;
    }

    public UUID getLiabilityId() {
        return liabilityId;
    }

    public Money getOriginalPaymentAmount() {
        return originalPaymentAmount;
    }

    public Money getAmountAppliedToLiability() {
        return amountAppliedToLiability;
    }

    public Money getCashOutflowInPortfolioCurrency() {
        return cashOutflowInPortfolioCurrency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LiabilityPaymentTransactionDetails that = (LiabilityPaymentTransactionDetails) o;
        return Objects.equals(this.liabilityId, that.liabilityId)
                && Objects.equals(this.originalPaymentAmount, that.originalPaymentAmount)
                && Objects.equals(this.amountAppliedToLiability, that.amountAppliedToLiability)
                && Objects.equals(this.cashOutflowInPortfolioCurrency, that.cashOutflowInPortfolioCurrency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.liabilityId, this.originalPaymentAmount, this.amountAppliedToLiability, this.cashOutflowInPortfolioCurrency);
    }
}