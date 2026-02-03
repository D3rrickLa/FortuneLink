package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import lombok.Builder;

/**
 * Immutable transaction record representing a financial event.
 * 
 * Key Design Principles:
 * - Transactions are historical facts (immutable)
 * - Trade execution is separate from cash flow
 * - All validation happens at construction
 * - Multi-currency fees are properly handled
 * 
 * @param id                   Unique transaction identifier
 * @param accountId            Account this transaction belongs to
 * @param transactionType      Type of transaction (BUY, SELL, etc.)
 * @param execution            Trade-specific details (null for non-trades)
 * @param cashDelta            Total cash impact in account's currency
 * @param fees                 List of fees associated with this transaction
 * @param occurredAt           When the transaction occurred
 * @param notes                Human-readable notes
 * @param relatedTransactionId Link to related transaction (e.g., DRIP, split)
 * @param metadata             Additional structured metadata
 */
@Builder(toBuilder = true)
public record Transaction(
        @NotNull TransactionId id,
        @NotNull AccountId accountId,
        @NotNull TransactionType transactionType,
        @Nullable TradeExecution execution,
        @NotNull Money cashDelta,
        @NotNull List<Fee> fees,
        @NotNull Instant occurredAt,
        @NotNull String notes,
        @Nullable TransactionId relatedTransactionId,
        @NotNull TransactionMetadata metadata) implements ClassValidation {

    public Transaction {
        // ===== Non-null validation =====
        Objects.requireNonNull(id, "Transaction ID cannot be null");
        Objects.requireNonNull(accountId, "Account ID cannot be null");
        Objects.requireNonNull(transactionType, "Transaction type cannot be null");
        Objects.requireNonNull(cashDelta, "Cash delta cannot be null");
        Objects.requireNonNull(fees, "Fees cannot be null");
        Objects.requireNonNull(occurredAt, "Occurred at cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null (use empty string)");
        Objects.requireNonNull(metadata, "Metadata cannot be null (use empty TransactionMetadata)");

        // ===== Defensive copies =====
        fees = List.copyOf(fees);
        notes = notes.trim();

        // ===== Domain invariants =====
        boolean requiresExecution = switch (transactionType) {
            case BUY, SELL, DIVIDEND_REINVEST, SPLIT -> true;
            case DEPOSIT, WITHDRAWAL, DIVIDEND, INTEREST, FEE, TRANSFER_IN, TRANSFER_OUT -> false;
        };

        if (requiresExecution && execution == null) {
            throw new IllegalArgumentException(
                    String.format("%s requires trade execution details", transactionType));
        }

        if (!requiresExecution && execution != null) {
            throw new IllegalArgumentException(
                    String.format("%s cannot have trade execution details", transactionType));
        }

        // ===== Consistency validation =====
        if (requiresExecution) {
            validateExecutionConsistency(transactionType, execution, cashDelta, fees);
        }

        // ===== Fee currency validation =====
        validateFeeCurrencies(fees, cashDelta.currency());
    }

    /**
     * Validates that the cash delta is consistent with the trade execution and
     * fees.
     * Prevents impossible transactions like buying at $150 but only paying $50.
     */
    private static void validateExecutionConsistency(
            TransactionType type,
            TradeExecution execution,
            Money cashDelta,
            List<Fee> fees) {
        Money grossValue = execution.grossValue();

        // Calculate total fees in transaction currency
        Money totalFees = fees.stream()
                .map(Fee::amountInTransactionCurrency)
                .reduce(Money.ZERO(cashDelta.currency()), Money::add);

        Money expectedCashDelta = switch (type) {
            case BUY -> {
                // Buy: cash out = -(gross + fees)
                yield grossValue.add(totalFees).negate();
            }
            case SELL -> {
                // Sell: cash in = gross - fees
                yield grossValue.subtract(totalFees);
            }
            case DIVIDEND_REINVEST, SPLIT -> {
                // No cash movement
                yield Money.ZERO(cashDelta.currency());
            }
            default -> throw new IllegalStateException("Unexpected transaction type: " + type);
        };

        // Allow small rounding differences (0.01)
        Money difference = cashDelta.subtract(expectedCashDelta).abs();
        Money tolerance = Money.of(BigDecimal.valueOf(0.01), cashDelta.currency());

        if (difference.isGreaterThan(tolerance)) {
            throw new IllegalArgumentException(String.format(
                    "Cash delta (%s) inconsistent with execution. Expected %s " +
                            "(gross value: %s, total fees: %s, type: %s)",
                    cashDelta, expectedCashDelta, grossValue, totalFees, type));
        }
    }

    /**
     * Validates that all fees can be converted to the transaction currency.
     * Ensures we won't get runtime errors during fee calculations.
     */
    private static void validateFeeCurrencies(List<Fee> fees, Currency transactionCurrency) {
        for (Fee fee : fees) {
            Money nativeAmount = fee.amountInNativeCurrency();

            // Same currency - no problem
            if (nativeAmount.currency().equals(transactionCurrency)) {
                continue;
            }

            // Different currency - must have exchange rate
            if (fee.exchangeRate() == null) {
                throw new IllegalArgumentException(String.format(
                        "Fee in %s requires exchange rate to convert to transaction currency %s",
                        nativeAmount.currency(), transactionCurrency));
            }

            // Verify the exchange rate can actually do the conversion
            if (!fee.exchangeRate().canConvert(nativeAmount.currency(), transactionCurrency)) {
                throw new IllegalArgumentException(String.format(
                        "Fee exchange rate cannot convert %s to %s",
                        nativeAmount.currency(), transactionCurrency));
            }
        }
    }

    /*
     * ======================================
     * Domain Queries - Public API
     * ======================================
     */

    /**
     * Calculates total fees in the transaction's currency.
     * Handles multi-currency fees by converting them at their recorded exchange
     * rates.
     * 
     * @return Total fees in transaction currency
     * @throws IllegalStateException if fee conversion fails
     */
    public Money calculateTotalFeesInTransactionCurrency() {
        Currency txCurrency = cashDelta.currency();

        return fees.stream()
                .map(fee -> convertFeeToTransactionCurrency(fee, txCurrency))
                .reduce(Money.ZERO(txCurrency), Money::add);
    }

    /**
     * Converts a single fee to the transaction currency.
     * Uses the fee's stored exchange rate if currencies differ.
     */
    private Money convertFeeToTransactionCurrency(Fee fee, Currency txCurrency) {
        Money nativeAmount = fee.amountInNativeCurrency();

        // Same currency - no conversion needed
        if (nativeAmount.currency().equals(txCurrency)) {
            return nativeAmount;
        }

        // Different currency - use fee's transaction amount (pre-converted)
        // This is more accurate than re-converting from native amount
        Money transactionAmount = fee.amountInTransactionCurrency();

        if (!transactionAmount.currency().equals(txCurrency)) {
            throw new IllegalStateException(String.format(
                    "Fee transaction amount currency (%s) doesn't match expected currency (%s)",
                    transactionAmount.currency(), txCurrency));
        }

        return transactionAmount;
    }

    /**
     * Net cash impact after accounting for fees.
     * This is the actual amount that moved in/out of your account.
     * 
     * For BUY: negative (cash out)
     * For SELL: positive (cash in)
     * For DEPOSIT: positive (cash in)
     * For WITHDRAWAL: negative (cash out)
     */
    public Money netCashImpact() {
        Money totalFeesInTxCurrency = calculateTotalFeesInTransactionCurrency();
        return cashDelta.subtract(totalFeesInTxCurrency);
    }

    /**
     * True if this is a trade (has execution details).
     */
    public boolean isTrade() {
        return execution != null;
    }

    /**
     * True if this transaction affects holdings (quantity changed).
     */
    public boolean affectsHoldings() {
        return isTrade() && execution.quantity().isNonZero();
    }

    /**
     * True if this increases holdings (quantity positive).
     */
    public boolean isIncrease() {
        return isTrade() && execution.quantity().isPositive();
    }

    /**
     * True if this decreases holdings (quantity negative).
     */
    public boolean isDecrease() {
        return isTrade() && execution.quantity().isNegative();
    }

    /**
     * True if this transaction generates taxable events.
     * Used for tax reporting and capital gains calculations.
     */
    public boolean isTaxable() {
        return transactionType.isTaxable();
    }

    /**
     * Cost basis impact for tax lot tracking.
     * Delegates to TransactionType for type-specific logic.
     */
    public Money costBasisDelta() {
        Money totalFeesInTxCurrency = calculateTotalFeesInTransactionCurrency();
        return transactionType.calculateCostBasisDelta(cashDelta, totalFeesInTxCurrency);
    }

    /**
     * Market price per unit (clean price, excluding fees).
     * This is what the exchange quoted.
     * 
     * @throws IllegalStateException if called on non-trade transaction
     */
    public Money marketPricePerUnit() {
        if (execution == null) {
            throw new IllegalStateException("Non-trade transaction has no price per unit");
        }
        return execution.pricePerUnit();
    }

    /**
     * Effective price per unit including fees.
     * This is your actual cost basis per share.
     * 
     * @throws IllegalStateException if called on non-trade or zero-quantity
     *                               transaction
     */
    public Money effectivePricePerUnit() {
        if (execution == null) {
            throw new IllegalStateException("Non-trade transaction has no price per unit");
        }

        if (execution.quantity().isZero()) {
            throw new IllegalStateException(
                    "Cannot calculate effective price per unit for zero quantity transaction");
        }

        // Total cost (including fees) ÷ quantity
        return cashDelta.abs().divide(execution.quantity().value().abs());
    }

    /**
     * Generates a human-readable audit log entry.
     * Useful for debugging and compliance reporting.
     */
    public String toAuditLog() {
        String tradeInfo = isTrade()
                ? String.format("%s %s @ %s",
                        execution.quantity(),
                        execution.asset().symbol(),
                        execution.pricePerUnit())
                : "NON-TRADE";

        String notesInfo = notes.isEmpty() ? "" : " | Notes: " + notes;

        return String.format(
                "[%s] %s | Account: %s | %s | Cash: %s | Fees: %s | Net: %s%s",
                occurredAt,
                transactionType,
                accountId.value(),
                tradeInfo,
                cashDelta,
                calculateTotalFeesInTransactionCurrency(),
                netCashImpact(),
                notesInfo);
    }

    /*
     * ======================================
     * Nested Records
     * ======================================
     */

    /**
     * Trade-specific execution details.
     * Only present for transactions that involve buying/selling assets.
     * 
     * Separated from the main Transaction record to:
     * - Enforce type safety (non-trades can't have execution)
     * - Group related fields
     * - Make the domain model clearer
     */
    public record TradeExecution(
            @NotNull AssetSymbol asset,
            @NotNull Quantity quantity,
            @NotNull Money pricePerUnit) {
        public TradeExecution {
            Objects.requireNonNull(asset, "Asset symbol cannot be null");
            Objects.requireNonNull(quantity, "Quantity cannot be null");
            Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null");

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
            return pricePerUnit.multiply(quantity.value().abs());
        }
    }

    /**
     * Strongly-typed metadata container.
     * Provides a clean API for accessing key-value data without exposing raw Map.
     * 
     * Common use cases:
     * - Tax lot IDs
     * - Exchange codes
     * - Order IDs
     * - Broker reference numbers
     */
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

    /*
     * ======================================
     * Factory Methods (Optional)
     * ======================================
     */

    /**
     * Creates a BUY transaction with automatic cash delta calculation.
     * 
     * Example:
     * 
     * <pre>
     * Transaction tx = Transaction.buy(
     *     accountId,
     *     new AssetSymbol("AAPL"),
     *     Quantity.of(10),
     *     Money.of(150, USD),
     *     List.of(new Fee(...)),
     *     Instant.now()
     * );
     * </pre>
     */
    public static Transaction buy(
            AccountId accountId,
            AssetSymbol asset,
            Quantity quantity,
            Money pricePerUnit,
            List<Fee> fees,
            Instant occurredAt) {
        return buy(accountId, asset, quantity, pricePerUnit, fees, occurredAt, "");
    }

    public static Transaction buy(
            AccountId accountId,
            AssetSymbol asset,
            Quantity quantity,
            Money pricePerUnit,
            List<Fee> fees,
            Instant occurredAt,
            String notes) {
        Money grossValue = pricePerUnit.multiply(quantity.value());

        Money totalFees = fees.stream()
                .map(Fee::amountInTransactionCurrency)
                .reduce(Money.ZERO(pricePerUnit.currency()), Money::add);

        Money cashDelta = grossValue.add(totalFees).negate();

        return Transaction.builder()
                .id(TransactionId.newId())
                .accountId(accountId)
                .transactionType(TransactionType.BUY)
                .execution(new TradeExecution(asset, quantity, pricePerUnit))
                .cashDelta(cashDelta)
                .fees(fees)
                .occurredAt(occurredAt)
                .notes(notes)
                .metadata(TransactionMetadata.empty())
                .build();
    }

    /**
     * Creates a SELL transaction with automatic cash delta calculation.
     */
    public static Transaction sell(
            AccountId accountId,
            AssetSymbol asset,
            Quantity quantity,
            Money pricePerUnit,
            List<Fee> fees,
            Instant occurredAt) {
        return sell(accountId, asset, quantity, pricePerUnit, fees, occurredAt, "");
    }

    public static Transaction sell(
            AccountId accountId,
            AssetSymbol asset,
            Quantity quantity,
            Money pricePerUnit,
            List<Fee> fees,
            Instant occurredAt,
            String notes) {
        // Quantity should be negative for sells
        Quantity sellQuantity = quantity.isNegative() ? quantity : quantity.negate();

        Money grossValue = pricePerUnit.multiply(quantity.value().abs());

        Money totalFees = fees.stream()
                .map(Fee::amountInTransactionCurrency)
                .reduce(Money.ZERO(pricePerUnit.currency()), Money::add);

        Money cashDelta = grossValue.subtract(totalFees);

        return Transaction.builder()
                .id(TransactionId.generate())
                .accountId(accountId)
                .transactionType(TransactionType.SELL)
                .execution(new TradeExecution(asset, sellQuantity, pricePerUnit))
                .cashDelta(cashDelta)
                .fees(fees)
                .occurredAt(occurredAt)
                .notes(notes)
                .metadata(TransactionMetadata.empty())
                .build();
    }

    /**
     * Creates a DEPOSIT transaction.
     */
    public static Transaction deposit(
            AccountId accountId,
            Money amount,
            Instant occurredAt) {
        return deposit(accountId, amount, occurredAt, "");
    }

    public static Transaction deposit(
            AccountId accountId,
            Money amount,
            Instant occurredAt,
            String notes) {
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        return Transaction.builder()
                .id(TransactionId.newId())
                .accountId(accountId)
                .transactionType(TransactionType.DEPOSIT)
                .execution(null)
                .cashDelta(amount)
                .fees(List.of())
                .occurredAt(occurredAt)
                .notes(notes)
                .metadata(TransactionMetadata.empty())
                .build();
    }

    /**
     * Creates a WITHDRAWAL transaction.
     */
    public static Transaction withdrawal(
            AccountId accountId,
            Money amount,
            Instant occurredAt) {
        return withdrawal(accountId, amount, occurredAt, "");
    }

    public static Transaction withdrawal(
            AccountId accountId,
            Money amount,
            Instant occurredAt,
            String notes) {
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        return Transaction.builder()
                .id(TransactionId.generate())
                .accountId(accountId)
                .transactionType(TransactionType.WITHDRAWAL)
                .execution(null)
                .cashDelta(amount.negate())
                .fees(List.of())
                .occurredAt(occurredAt)
                .notes(notes)
                .metadata(TransactionMetadata.empty())
                .build();
    }
}