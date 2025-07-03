package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.math.BigDecimal;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public final class CashflowTransactionDetails extends TransactionDetails{
    private final Money originalCashflowAmount;
    private final Money convertedCashflowAmount;
    private final BigDecimal exchangeRate;
    private final Money exchangeRateFee;
    private final Money otherFees;
    
    public CashflowTransactionDetails(Money originalCashflowAmount, Money convertedCashflowAmount, BigDecimal exchangeRate, Money exchangeRateFee, Money otherFees) {
        Objects.requireNonNull(originalCashflowAmount, "Original cashflow amount cannot be null.");
        Objects.requireNonNull(convertedCashflowAmount, "Converted cashflow amount cannot be null.");
        Objects.requireNonNull(exchangeRate, "Exchange rate cannot be null.");
        Objects.requireNonNull(exchangeRateFee, "Exchange rate fee cannot be null.");
        Objects.requireNonNull(otherFees, "Other fees amount cannot be null.");
 
        if (originalCashflowAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Original cashflow amount must be positive.");
        }
        if (convertedCashflowAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Converted cashflow amount must be positive."); // Or adjust based on type (withdrawal might be negative representation)
        }
        if (exchangeRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Exchange rate must be positive.");
        }
        // Fee amounts can be zero if no fee was applied, so non-negative is fine.
        if (exchangeRateFee.amount().compareTo(BigDecimal.ZERO) < 0 || otherFees.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee amounts cannot be negative.");
        }

        // Ensure currencies are correct for audit
        if (!convertedCashflowAmount.currency().equals(exchangeRateFee.currency()) || 
            !convertedCashflowAmount.currency().equals(otherFees.currency())) {
            throw new IllegalArgumentException("Converted cashflow amount and all fees in CashTransactionDetails must be in the same portfolio currency.");
        }   
        
        this.originalCashflowAmount = originalCashflowAmount;
        this.convertedCashflowAmount = convertedCashflowAmount;
        this.exchangeRate = exchangeRate;
        this.exchangeRateFee = exchangeRateFee;
        this.otherFees = otherFees;
    }

    public Money getOriginalCashflowAmount() {
        return originalCashflowAmount;
    }

    public Money getConvertedCashflowAmount() {
        return convertedCashflowAmount;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public Money getExchangeRateFee() {
        return exchangeRateFee;
    }

    public Money getOtherFees() {
        return otherFees;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CashflowTransactionDetails that = (CashflowTransactionDetails) o;
        return Objects.equals(this.originalCashflowAmount, that.originalCashflowAmount) 
            && Objects.equals(this.convertedCashflowAmount, that.convertedCashflowAmount) 
            && Objects.equals(this.exchangeRate, that.exchangeRate) 
            && Objects.equals(this.exchangeRateFee, that.exchangeRateFee) 
            && Objects.equals(this.otherFees, that.otherFees);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.originalCashflowAmount, this.convertedCashflowAmount, this.exchangeRate, this.exchangeRateFee, this.otherFees);
    }
    
    
}