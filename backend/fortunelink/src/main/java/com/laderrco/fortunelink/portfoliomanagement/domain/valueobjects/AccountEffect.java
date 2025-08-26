package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AccountMetadataKey;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;

// holds all the information needed for the various Income/Expense Transaction Details
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
            throw new IllegalArgumentException("Both gross and new amount cannot be zero.");
        }

        if (!grossAmount.nativeAmount().currency().equals(netAmount.nativeAmount().currency())) {
            throw new CurrencyMismatchException("Gross and net amoutns must be in the same currency.");
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

    public boolean hasFees() {
        return !getFeeAmount().nativeAmount().isZero();
    }

    public boolean hasWithholdingTax() {
        return metadata.containsKey(AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey());
    }
    
    public Optional<BigDecimal> getWithholdingTaxRate() {
        return getMetadata(AccountMetadataKey.WITHHOLDING_TAX_RATE)
        .map(BigDecimal::new);
    }

    public MonetaryAmount getWitholdingTaxAmount() {
        return getWithholdingTaxRate()
            .map(rate -> grossAmount.multiply(rate))
            .orElse(MonetaryAmount.ZERO(grossAmount.nativeAmount().currency()));
    }

    // Business logic based on cashflow type
    public boolean isIncomeTransaction() {
        return cashflowType == CashflowType.DIVIDEND || 
            cashflowType == CashflowType.INTEREST ||
            cashflowType == CashflowType.RENTAL_INCOME ||
            cashflowType == CashflowType.OTHER_INCOME;
    }

    public boolean isExpenseTransaction() {
        return cashflowType == CashflowType.WITHDRAWAL || 
            cashflowType == CashflowType.FEE ||
            cashflowType == CashflowType.OTHER_OUTFLOW;
    }

    public boolean isTransferTransaction() {
        return cashflowType == CashflowType.TRANSFER;
    }

    public boolean isDepositTransaction() {
        return cashflowType == CashflowType.DEPOSIT;
    }

    public boolean isUnknownTransaction() {
        return cashflowType == CashflowType.UNKNOWN;
    }

    // Impact analysis
    public boolean isPositiveImpact() {
        return netAmount.nativeAmount().isPositive();
    }

    public boolean isNegativeImpact() {
        return netAmount.nativeAmount().isNegative();
    }

    // Multi-currency support
    public boolean isMultiCurrency() {
        return grossAmount.isMultiCurrency() || netAmount.isMultiCurrency();
    }

    // Tax and reporting
    public boolean requiresTaxReporting() {
        return switch (cashflowType) {
            case DIVIDEND, INTEREST, RENTAL_INCOME -> true; // Always taxable
            case OTHER_INCOME -> hasWithholdingTax(); // Maybe taxable
            case WITHDRAWAL -> false; // Usually not taxable events
            case FEE, OTHER_OUTFLOW -> false; // Expenses, not taxable income
            case DEPOSIT, TRANSFER -> false; // Moving money, not taxable
            case UNKNOWN -> hasWithholdingTax(); // Conservative approach
            default -> false;
        };
    }
   
    // validation helper
    public boolean isValidForCashflowType() {
        return switch (this.cashflowType) {
            case DIVIDEND, INTEREST -> isPositiveImpact(); // Income should be positive
            case WITHDRAWAL, FEE, OTHER_OUTFLOW -> isNegativeImpact();    // Expenses should be negative
            case DEPOSIT -> isPositiveImpact();            // Deposits should be positive
            case TRANSFER -> true; // Transfers can be either direction
            case UNKNOWN -> true; // Unknown types bypass validation (or you could be stricter)
            default -> throw new IllegalArgumentException("Unexpected value: " + this.cashflowType);
        };
    }
    
    private static void validateAmountsForCashflowType(CashflowType type, MonetaryAmount gross, MonetaryAmount net) {
        switch (type) {
            case DIVIDEND, INTEREST, RENTAL_INCOME, OTHER_INCOME -> {
                if (gross.isNegative() || net.isNegative()) {
                    throw new IllegalArgumentException("Income transactions must have positive amounts");
                }
            }
            case WITHDRAWAL, FEE, OTHER_OUTFLOW -> {
                if (gross.isPositive() || net.isPositive()) {
                    throw new IllegalArgumentException("Expense transactions must have negative amounts");
                }
            }
            case DEPOSIT -> {
                if (gross.isNegative() || net.isNegative()) {
                    throw new IllegalArgumentException("Deposit transactions must have positive amounts");
                }
            }
            case TRANSFER -> {
                // Transfers can be either direction, so no validation needed
                // Could be money moving between accounts (+ in one, - in another)
            }
            case UNKNOWN -> {
                // Option 1: No validation (lenient approach)
                // Option 2: Require amounts to be non-zero but allow either direction
                if (gross.isZero() && net.isZero()) {
                    throw new IllegalArgumentException("Unknown transactions must have non-zero amounts");
                }
            }
            default -> throw new IllegalArgumentException("Unexpected cashflow type: " + type);
        }
    }

    private Optional<String> getMetadata(AccountMetadataKey key) {
        return Optional.ofNullable(metadata.get(key.getKey()));
    }
}
