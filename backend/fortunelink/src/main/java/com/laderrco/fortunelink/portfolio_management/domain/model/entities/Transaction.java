package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

/*



*/
public record Transaction(
        TransactionId id,
        AccountId accountId,
        TransactionType transactionType,
        AssetSymbol assetSymbol, // nullable only if quantityDelta == 0
        Quantity quantityDelta,
        Money cashDelta,
        List<Fee> fees,
        Instant occurredAt,
        String notes,

        // optional
        TransactionId relatedTransactionId,
        Map<String, String> metadata) implements ClassValidation {

    public Transaction {
        ClassValidation.validateParameter(id);
        ClassValidation.validateParameter(accountId);
        ClassValidation.validateParameter(transactionType);
        ClassValidation.validateParameter(quantityDelta);
        ClassValidation.validateParameter(cashDelta);
        ClassValidation.validateParameter(fees);
        ClassValidation.validateParameter(occurredAt);

        notes = notes.trim();
        fees = List.copyOf(fees);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);

        validateInvariants(transactionType, assetSymbol, quantityDelta, cashDelta);
    }

    private static void validateInvariants(
            TransactionType type,
            AssetSymbol asset,
            Quantity qty,
            Money cash) {
        if (qty.isNonZero() && asset == null) {
            throw new IllegalArgumentException(
                    "Asset symbol required when quantity changes");
        }

        if (qty.isZero() && cash.isZero()) {
            throw new IllegalArgumentException(
                    "Transaction must affect cash or holdings");
        }

        // Optional semantic checks (safe to keep)
        switch (type) {
            case BUY -> {
                if (!qty.isPositive())
                    throw new IllegalArgumentException("BUY quantity must be positive");
                if (!cash.isNegative())
                    throw new IllegalArgumentException("BUY must reduce cash");
            }
            case SELL -> {
                if (!qty.isNegative())
                    throw new IllegalArgumentException("SELL quantity must be negative");
                if (!cash.isPositive())
                    throw new IllegalArgumentException("SELL must increase cash");
            }
            case DEPOSIT -> {
                if (qty.isNonZero())
                    throw new IllegalArgumentException("DEPOSIT cannot affect holdings");
                if (!cash.isPositive())
                    throw new IllegalArgumentException("DEPOSIT must increase cash");
            }
            case WITHDRAWAL -> {
                if (qty.isNonZero())
                    throw new IllegalArgumentException("WITHDRAWAL cannot affect holdings");
                if (!cash.isNegative())
                    throw new IllegalArgumentException("WITHDRAWAL must decrease cash");
            }
            case DIVIDEND, INTEREST -> {
                if (!cash.isPositive())
                    throw new IllegalArgumentException(type + " must increase cash");
                // Dividend can be either cash-only OR reinvested (quantity > 0)
            }
            case FEE -> {
                if (!cash.isNegative())
                    throw new IllegalArgumentException("FEE must decrease cash");
            }
            // case SPLIT -> {
            // if (!qty.isNonZero())
            // throw new IllegalArgumentException("SPLIT must affect holdings");
            // if (!cash.isZero())
            // throw new IllegalArgumentException("SPLIT should not affect cash");
            // }
            case TRANSFER_IN -> {
                // Can affect both cash and holdings depending on transfer type
            }
            case TRANSFER_OUT -> {
                // Can affect both cash and holdings depending on transfer type
            }
            default -> {

            }
        }
    }

    /* -------- Domain Queries -------- */

    public Money netCashImpact() {
        return cashDelta.subtract(totalFees());
    }

    public Money totalFees() {
        if (fees.isEmpty()) {
            return Money.ZERO(cashDelta.currency());
        }
        return fees.stream()
                .map(Fee::amountInNativeCurrency)
                .reduce(Money.ZERO(cashDelta.currency()), Money::add);
    }

    public boolean affectsHoldings() {
        return quantityDelta.isNonZero();
    }

    public boolean isIncrease() {
        return quantityDelta.isPositive();
    }

    public boolean isDecrease() {
        return quantityDelta.isNegative();
    }

    public boolean isTaxable() {
        return transactionType.isTaxable();
    }

    public Money costBasisDelta() {
        return transactionType.calculateCostBasisDelta(cashDelta, totalFees());
    }
}