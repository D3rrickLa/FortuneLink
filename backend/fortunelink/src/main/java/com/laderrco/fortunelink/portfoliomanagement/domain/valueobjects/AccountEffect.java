package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;

// holds all the information needed for the various Cash/Income/Expense Transaction Details
public record AccountEffect(
    MonetaryAmount grossAmount,
    MonetaryAmount netAmount,
    CashflowType cashflowType,
    Map<String, String> metadata
) {
    public AccountEffect {
        Objects.requireNonNull(grossAmount, "Gross amount cannot be null.");
        Objects.requireNonNull(netAmount, "Net amount cannot be null.");
        Objects.requireNonNull(cashflowType, "Cashflow type cannot be null.");
        
        if (grossAmount.nativeAmount().isZero() && netAmount.nativeAmount().isZero()) {
            throw new IllegalArgumentException("Both gross and net amount cannot be zero.");
        }

        if (!grossAmount.nativeAmount().currency().equals(netAmount.nativeAmount().currency())) {
            throw new CurrencyMismatchException("Gross and net amounts must be in the same currency.");
        }

        if (!grossAmount.conversion().toCurrency().equals(netAmount.conversion().toCurrency())) {
            throw new CurrencyMismatchException("Gross and net amounts must have same portfolio currency.");
        }

        metadata = metadata == null ? Collections.emptyMap() : Map.copyOf(metadata);
        
        validateAmountsForCashflowType(cashflowType, grossAmount, netAmount);
    }

    public MonetaryAmount getFeeAmount() {
        return grossAmount.subtract(netAmount).abs();
    }

    private static final void validateAmountsForCashflowType(CashflowType type, MonetaryAmount gross, MonetaryAmount net) {
        switch (type) {
            case  DIVIDEND, INTEREST, RENTAL_INCOME, OTHER_INCOME -> {
                if (gross.isNegative() || net.isNegative()) {
                    throw new IllegalArgumentException("Income transaction must have positive amounts.");
                }
            }
            case WITHDRAWAL, FEE, OTHER_OUTFLOW -> {

                if (gross.isPositive() || net.isPositive()) {
                    throw new IllegalArgumentException("Expense transactions must have negative amount");
                }
            }
            case DEPOSIT -> {
                if (gross.isNegative() || net.isNegative()) {
                    throw new IllegalArgumentException("Deposit transactions must have positive amounts");
                }               
            }
            case TRANSFER -> {
                // Transfer can be either direction, no validation
            }
            case UNKNOWN -> {
                // No additional validation - allow any non-zero amounts         
            }
        
            default -> throw new IllegalArgumentException("Unexpected cashflow type: " + type);
        }
    }
}
