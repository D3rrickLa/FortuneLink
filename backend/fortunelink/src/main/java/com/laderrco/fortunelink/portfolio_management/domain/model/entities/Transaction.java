package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.TradeExecution;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
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
                         // fees baked in and is the ONLY source of truth
                         // this is also why we don't need an exchange rate for trading
                         // we should have already known how much USD 1000 cost us
        List<Fee> fees, // Fees should already have an account currency conversion if needed
        Instant occurredAt,
        String notes,
        TransactionId relatedTransactionId, // nullable
        TransactionMetadata metadata) // nullable
        implements ClassValidation {

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
        if (execution == null) {
            // non-trade transactions don't need deep validation
            return;
        }
        // Validate cash delta sign matches transaction type
        boolean cashIsNegative = cashDelta.isNegative();
        boolean expectNegative = transactionType == TransactionType.BUY;

        if (cashIsNegative != expectNegative) {
            throw new IllegalArgumentException(
                    String.format("%s transaction should have %s cashDelta, got: %s",
                            transactionType,
                            expectNegative ? "negative" : "positive",
                            cashDelta));
        }

        Money totalFees = calculateTotalFeesInAccountCurrency();
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