package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;
// we can't derived the cashDelta, that is with a mindset from trading only, what if we get charged only a fee?
// that is where cashDelta comes in
// NOTE: NO CALCULATIONS HERE, JSUT ASSERTIONS
public record Transaction(
        TransactionId transactionId,
        AccountId accountId,
        TransactionType transactionType,
        TradeExecution execution, // nullable
        Money cashDelta, // ALWAYS Account Currency, is the final settled amount that hit the account,
                         // fees backed in and is the ONLY source of truth
                         // this is also we why don't need an exchange rate for trading
                         // we should have already known how much USD 1000 cost us
        List<Fee> fees, // Fees should already have an account currency conversion if needed
        Instant occurredAt,
        String notes,
        TransactionId relatedTransactionId,
        TransactionMetadata metadata) implements ClassValidation {

    public Transaction {
        ClassValidation.validateParameter(transactionId);
        ClassValidation.validateParameter(accountId);
        ClassValidation.validateParameter(transactionType);
        ClassValidation.validateParameter(cashDelta);
        ClassValidation.validateParameter(fees);
        ClassValidation.validateParameter(occurredAt);
        ClassValidation.validateParameter(notes);

        fees = List.copyOf(fees);
        notes = notes.trim();
        boolean requiresExecution = switch (transactionType) {
            case BUY, SELL -> true;
            default -> false;
        };

        if (requiresExecution && execution == null) {
            throw new IllegalArgumentException(transactionType + " requires execution details");
        }

        if (!requiresExecution && execution != null) {
            throw new IllegalArgumentException(transactionType + " cannot have execution details");
        }

        validateCashConsistency();
    }

    public Money calculateTotalFeesInAccountCurrency() {
        Currency accountCurrency = cashDelta.currency();

        return fees.stream()
                .map(Fee::accountAmount)
                .reduce(Money.ZERO(accountCurrency), Money::add);
    }

    public Money netCashImpact() {
        return cashDelta;
    }

    public boolean isTrade() {
        return execution != null;
    }

    private void validateCashConsistency() {
        Money totalFees = calculateTotalFeesInAccountCurrency();
        if (execution == null) {
            // non-trade transactions don't need deep validation
            return;
        }
        Money gross = execution.grossValue();

        Money expected = switch (transactionType) {
            case BUY -> gross.add(totalFees).negate();
            case SELL -> gross.subtract(totalFees);
            default -> throw new IllegalArgumentException(
                    "Unexpected transaction type with execution: " + transactionType);
        };

        Money diff = cashDelta.subtract(expected).abs();
        Money tolerance = new Money(BigDecimal.valueOf(0.01), cashDelta.currency());

        if (diff.exceeds(tolerance)) {
            throw new IllegalArgumentException(String.format(
                    "Cash delta (%s) doesn't match expected (%s). " +
                            "Difference: %s (tolerance: %s)",
                    cashDelta, expected, diff, tolerance));
        }
    }

    // know what was traded, not the account who paid for it
    public record TradeExecution(AssetSymbol asset, Quantity quantity, Money pricePerUnit) {
        public TradeExecution {
            ClassValidation.validateParameter(asset, "Asset symbol cannot be null");
            ClassValidation.validateParameter(quantity, "Quantity cannot be null");
            ClassValidation.validateParameter(pricePerUnit, "Price per unit cannot be null");

            // Domain validation
            if (quantity.isZero()) {
                throw new IllegalArgumentException("Trade quantity cannot be zero");
            }

            if (pricePerUnit.isNegative()) {
                throw new IllegalArgumentException(
                        "Price per unit cannot be negative (got: " + pricePerUnit + ")");
            }
        }

        /**
         * Gross value of the trade before fees.
         * This is quantity × price, representing the market value.
         */
        public Money grossValue() {
            return pricePerUnit.multiply(quantity.amount().abs());
        }
    }

    public record TransactionMetadata(Map<String, String> values) {
        public TransactionMetadata {
            values = values == null ? Map.of() : Map.copyOf(values);
        }

        public static TransactionMetadata empty() {
            return new TransactionMetadata(Map.of());
        }

        public String get(String key) {
            return values.get(key);
        }

        public String getOrDefault(String key, String defaultValue) {
            return values.getOrDefault(key, defaultValue);
        }

        public TransactionMetadata with(String key, String value) {
            Map<String, String> copy = new HashMap<>(values);
            copy.put(key, value);
            return new TransactionMetadata(copy);
        }

        public TransactionMetadata withAll(Map<String, String> additionalMetadata) {
            Map<String, String> copy = new HashMap<>(values);
            copy.putAll(additionalMetadata);
            return new TransactionMetadata(copy);
        }

        public boolean containsKey(String key) {
            return values.containsKey(key);
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }
    }
}

// /**
// * Returns the absolute cost for ACB tracking.
// * For BUY: returns positive total cost (gross + fees)
// * For SELL: throws exception (sells don't contribute to ACB)
// */
// public Money acbContribution() {
// if (transactionType != TransactionType.BUY) {
// throw new IllegalStateException(
// "Only BUY transactions contribute to ACB"
// );
// }
// return cashDelta.abs();
// }

// /**
// * Returns the net proceeds for capital gains calculation.
// * For SELL: returns positive proceeds (gross - fees)
// * For BUY: throws exception
// */
// public Money saleProceeds() {
// if (transactionType != TransactionType.SELL) {
// throw new IllegalStateException(
// "Only SELL transactions generate proceeds"
// );
// }
// return cashDelta; // Already positive for sells
// }