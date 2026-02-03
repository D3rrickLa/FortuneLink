package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
// VO probably shouldn’t implement the validator interface; just call validation
// statically.
public record Fee(
        @NotNull FeeType feeType,
        @NotNull Money nativeAmount,
        @NotNull Money transactionAmount,
        @NotNull ExchangeRate appliedRate,
        @NotNull Instant occurredAt,
        @NotNull Map<String, String> metadata) implements ClassValidation {

    public Fee {
        ClassValidation.validateParameter(feeType, "Fee type cannot be null");
        ClassValidation.validateParameter(nativeAmount, "Native amount cannot be null");
        ClassValidation.validateParameter(transactionAmount, "Transaction amount cannot be null");
        ClassValidation.validateParameter(appliedRate, "Applied rate cannot be null");
        ClassValidation.validateParameter(occurredAt, "Occurred at cannot be null");

        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);

        validateConsistency(nativeAmount, transactionAmount, appliedRate);
    }

    private static void validateConsistency(
            Money nativeAmount,
            Money transactionAmount,
            ExchangeRate appliedRate) {
        // Same currency → no conversion needed
        if (nativeAmount.currency().equals(transactionAmount.currency())) {
            if (!nativeAmount.equals(transactionAmount)) {
                throw new IllegalArgumentException(
                        "Native and transaction amounts must match when currencies are the same");
            }
            return;
        }

        // Different currencies → validate conversion
        Money converted = appliedRate.convert(nativeAmount);
        Money difference = converted.subtract(transactionAmount).abs();

        // Allow 0.01 rounding difference
        if (difference.isGreaterThan(Money.of(0.01, transactionAmount.currency()))) {
            throw new IllegalArgumentException(String.format(
                    "Transaction amount (%s) doesn't match converted native amount (%s). " +
                            "Expected %s at rate %s",
                    transactionAmount, nativeAmount, converted, appliedRate));
        }
    }

    /** For tax/cost basis calculations */
    public Money amountInNativeCurrency() {
        return nativeAmount;
    }

    /** For cash flow/account reconciliation */
    public Money amountInTransactionCurrency() {
        return transactionAmount;
    }

    /** Convert to arbitrary currency for reporting */
    public Money convertTo(Currency target, ExchangeRate rate) {
        // Use native amount as source of truth
        if (nativeAmount.currency().equals(target)) {
            return nativeAmount;
        }
        return rate.convert(nativeAmount);
    }
}