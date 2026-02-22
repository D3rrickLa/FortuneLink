package com.laderrco.fortunelink.portfolio.application.services;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionDate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;

/**
 * Records transactions against an account by:
 * 1. Mutating account state (positions, cash balance)
 * 2. Constructing and returning an immutable Transaction record
 *
 * The caller (TransactionService) is responsible for persisting both
 * the mutated portfolio and the returned Transaction.
 *
 * No repositories, no market data. Pure domain logic.
 */
@Service
public class TransactionRecordingServiceImpl implements TransactionRecordingService {

    @Override
    public Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type,
            Quantity quantity, Price price, List<Fee> fees, String notes, Instant date) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        Currency currency = account.getAccountCurrency();
        Money feeTotal = Fee.totalInAccountCurrency(fees, currency);
        List<Fee> finalFees = feeTotal.isZero() ? List.of() : (fees != null ? fees : List.of());

        Money grossCost = price.pricePerUnit().multiply(quantity.amount());
        Money totalOutflow = grossCost.add(feeTotal);

        Position current = account.ensurePosition(symbol, type);
        ApplyResult<? extends Position> result = current.buy(quantity, grossCost, date);
        account.updatePosition(symbol, result.newPosition());
        account.withdraw(totalOutflow, "BUY " + symbol.value());

        Money cashDelta = totalOutflow.negate();

        return new Transaction(
                TransactionId.newId(),
                account.getAccountId(),
                TransactionType.BUY,
                new TradeExecution(symbol, quantity, price),
                null, // no split details
                cashDelta,
                finalFees,
                notes.trim(),
                TransactionDate.of(date),
                null, // no related transaction
                TransactionMetadata.manual(type));
    }

    @Override
    public Transaction recordSell(Account account, AssetSymbol symbol,
            Quantity quantity, Price price, List<Fee> fees, String notes, Instant date) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        Currency currency = account.getAccountCurrency();
        Money feeTotal = Fee.totalInAccountCurrency(fees, currency);
        List<Fee> finalFees = feeTotal.isZero() ? List.of() : (fees != null ? fees : List.of());

        Position current = account.getPosition(symbol)
                .orElseThrow(() -> new IllegalStateException("No position for symbol: " + symbol.value()));

        Money grossProceeds = price.pricePerUnit().multiply(quantity.amount());
        Money netProceeds = grossProceeds.subtract(feeTotal);

        ApplyResult<? extends Position> result = current.sell(quantity, grossProceeds, date);
        account.updatePosition(symbol, result.newPosition());
        account.deposit(netProceeds, "SELL " + symbol.value());

        // cashDelta is positive — cash comes IN from the sale
        Money cashDelta = netProceeds;

        // Get asset type from the existing position
        AssetType type = current.type();

        return new Transaction(
                TransactionId.newId(),
                account.getAccountId(),
                TransactionType.SELL,
                new TradeExecution(symbol, quantity, price),
                null,
                cashDelta,
                finalFees,
                notes.trim(),
                TransactionDate.of(date),
                null,
                TransactionMetadata.manual(type));
    }

    /**
     * Records a cash deposit — no position changes, just adds to cash balance.
     */
    @Override
    public Transaction recordDeposit(Account account, Money amount, String notes, Instant date) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        account.deposit(amount, "DEPOSIT");

        return new Transaction(
                TransactionId.newId(),
                account.getAccountId(),
                TransactionType.DEPOSIT,
                null, // no execution
                null,
                amount, // cashDelta is positive
                List.of(), // deposits have no fees
                notes.trim(),
                TransactionDate.of(date),
                null,
                TransactionMetadata.manual(AssetType.CASH));
    }

    /**
     * Records a cash withdrawal — no position changes, just reduces cash balance.
     */
    @Override
    public Transaction recordWithdrawal(Account account, Money amount, String notes, Instant date) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        account.withdraw(amount, "WITHDRAWAL");

        return new Transaction(
                TransactionId.newId(),
                account.getAccountId(),
                TransactionType.WITHDRAWAL,
                null,
                null,
                amount.negate(), // cashDelta is negative — cash leaves
                List.of(),
                notes.trim(),
                TransactionDate.of(date),
                null,
                TransactionMetadata.manual(AssetType.CASH));
    }

    /**
     * Records a platform/brokerage fee — deducts from cash, no position change.
     */
    @Override
    public Transaction recordFee(Account account, Money amount, String notes, Instant date) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        account.applyFee(amount, "FEE");

        return new Transaction(
                TransactionId.newId(),
                account.getAccountId(),
                TransactionType.FEE,
                null,
                null,
                amount.negate(), // cash leaves
                List.of(),
                notes.trim(),
                TransactionDate.of(date),
                null,
                TransactionMetadata.manual(AssetType.CASH));
    }

    /**
     * Records a dividend payment — credits cash, no position change.
     * The symbol is tracked for tax reporting purposes (taxable income).
     */
    @Override
    public Transaction recordDividend(Account account, AssetSymbol symbol, Money amount, String notes, Instant date) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        account.deposit(amount, "DIVIDEND from " + symbol.value());

        // Resolve asset type from existing position if available, default to STOCK
        AssetType type = account.getPosition(symbol)
                .map(Position::type)
                .orElse(AssetType.STOCK);

        return new Transaction(
                TransactionId.newId(),
                account.getAccountId(),
                TransactionType.DIVIDEND,
                null, // DIVIDEND does not requiresExecution per TransactionType enum
                null,
                amount, // cash comes in
                List.of(),
                notes.trim(),
                TransactionDate.of(date),
                null,
                TransactionMetadata.manual(type));
    }

    /**
     * Records a dividend reinvestment (DRIP):
     * - No cash movement (NONE impact per TransactionType enum)
     * - Increases position using the dividend proceeds
     */
    @Override
    public Transaction recordDividendReinvestment(Account account, AssetSymbol symbol,
            Quantity quantity, Price price, String notes, Instant date) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");

        Money totalCost = price.pricePerUnit().multiply(quantity.amount());

        // Resolve or create the position
        AssetType type = account.getPosition(symbol)
                .map(Position::type)
                .orElse(AssetType.STOCK);

        Position current = account.ensurePosition(symbol, type);
        ApplyResult<? extends Position> result = current.buy(quantity, totalCost, date);
        account.updatePosition(symbol, result.newPosition());

        // DIVIDEND_REINVEST has CashImpact.NONE — cashDelta must be zero
        Money zeroCashDelta = Money.ZERO(account.getAccountCurrency());

        return new Transaction(
                TransactionId.newId(),
                account.getAccountId(),
                TransactionType.DIVIDEND_REINVEST,
                new TradeExecution(symbol, quantity, price),
                null,
                zeroCashDelta,
                List.of(),
                notes.trim(),
                TransactionDate.of(date),
                null,
                TransactionMetadata.manual(type));
    }

    /**
     * Replays a single transaction against an account.
     * Used for position recalculation when transactions are excluded/restored.
     *
     * Only BUY, SELL, and SPLIT affect positions. Everything else is a cash event
     * handled at the Account level, not position level.
     */
    @Override
    public void replayTransaction(Account account, Transaction tx) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(tx, "Transaction cannot be null");

        if (tx.isExcluded()) {
            return; // excluded transactions don't contribute to state
        }

        switch (tx.transactionType()) {
            case BUY -> {
                AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
                Position current = account.ensurePosition(tx.execution().asset(), type);
                Money grossCost = tx.execution().grossValue(); // qty × price, no fees
                ApplyResult<? extends Position> result = current.buy(
                        tx.execution().quantity(),
                        // recordBuy passes frossCost to current.buy() only
                        // and not gross + fees
                        // when we didd it like this before,
                        // the backed fees will overstates ACB/costbasis
                        // ACB for tax purposes is gross cost only? fees are a separate deduction
                        // tx.cashDelta().abs(),
                        grossCost,
                        tx.occurredAt().timestamp());
                account.updatePosition(tx.execution().asset(), result.newPosition());
            }
            case SELL -> {
                account.getPosition(tx.execution().asset()).ifPresent(position -> {
                    ApplyResult<? extends Position> result = position.sell(
                            tx.execution().quantity(),
                            tx.cashDelta(),
                            tx.occurredAt().timestamp());
                    account.updatePosition(tx.execution().asset(), result.newPosition());
                });
            }
            case SPLIT -> {
                account.getPosition(tx.execution().asset()).ifPresent(position -> {
                    ApplyResult<? extends Position> result = position.split(tx.split().ratio());
                    account.updatePosition(tx.execution().asset(), result.newPosition());
                });
            }
            case DEPOSIT, TRANSFER_IN -> account.deposit(tx.cashDelta(), tx.transactionType().name());
            case WITHDRAWAL, TRANSFER_OUT -> account.withdraw(tx.cashDelta().abs(), tx.transactionType().name());
            case FEE -> account.applyFee(tx.cashDelta().abs(), "FEE replay");
            case DIVIDEND, INTEREST -> account.deposit(tx.cashDelta(), tx.transactionType().name());
            case DIVIDEND_REINVEST -> {
                // Re-apply position increase, no cash movement
                AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
                Position current = account.ensurePosition(tx.execution().asset(), type);
                Money totalCost = tx.execution().pricePerUnit().pricePerUnit()
                        .multiply(tx.execution().quantity().amount());
                ApplyResult<? extends Position> result = current.buy(
                        tx.execution().quantity(), totalCost, tx.occurredAt().timestamp());
                account.updatePosition(tx.execution().asset(), result.newPosition());
            }
            default -> {
                // RETURN_OF_CAPITAL, REINVESTED_CAPITAL_GAIN, OTHER — no position or cash
                // effect
            }
        }
    }

}
