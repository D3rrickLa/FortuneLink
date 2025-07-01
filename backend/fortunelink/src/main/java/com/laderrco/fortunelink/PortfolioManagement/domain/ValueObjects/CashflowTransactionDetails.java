package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

// this suceeds the CashTransactionDetails.java class
public class CashflowTransactionDetails extends TransactionDetails{
     private final Money originalCashflowAmount;        // e.g., $100 USD (the 'cashflowAmount' parameter you pass in)
    private final Money convertedCashflowAmount;       // e.g., $135 CAD (the amount actually hitting the portfolio cash balance before fees)
    private final BigDecimal exchangeRate;             // e.g., 1.35
    private final Money exchangeRateFee;               // e.g., $3 CAD (the explicit fee for the FX conversion, parsed from input 'fees')
    private final Money otherFees;                     // e.g., $2.70 CAD (sum of other fees like withdrawal/deposit fees, etc., already converted)
    
    // AI generated code
    public CashflowTransactionDetails(Money originalCashflowAmount, Money convertedCashflowAmount, BigDecimal exchangeRate, Money exchangeRateFee, Money otherFees) {
        
        Objects.requireNonNull(originalCashflowAmount, "Original cashflow amount cannot be null.");
        Objects.requireNonNull(convertedCashflowAmount, "Converted cashflow amount cannot be null.");
        Objects.requireNonNull(exchangeRate, "Exchange rate cannot be null.");
        Objects.requireNonNull(exchangeRateFee, "Exchange rate fee cannot be null.");
        Objects.requireNonNull(otherFees, "Other fees amount cannot be null.");


        // Basic validations
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

    // public Money netImpactOnPortfolioCash() {
    //     // Handle positive/negative based on TransactionType if stored in Transaction
    //     // For simplicity, assuming these amounts are positive and will be added/subtracted by the Portfolio
    //     return convertedCashflowAmount.subtract(exchangeRateFee).subtract(otherFees);
    // }
    
}
