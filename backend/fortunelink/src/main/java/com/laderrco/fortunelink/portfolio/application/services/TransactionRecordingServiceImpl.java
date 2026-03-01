package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Records transactions against an account by:
 * 1. Mutating account state (positions, cash balance)
 * 2. Constructing and returning an immutable Transaction record
 * <p>
 * The caller (TransactionService) is responsible for persisting both
 * the mutated portfolio and the returned Transaction.
 * <p>
 * No repositories, no market data. Pure domain logic.
 */

// NOTE: the record buy and stuff might be wrong for CAB ACB
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

        // grossCost = qty × price, fees excluded intentionally.
        // Position cost basis tracks
        // gross cost only (correct for ACB/FIFO tax// purposes).
        // Cash outflow includes fees (totalOutflow) — these are separate concerns.
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

        Money cashDelta = netProceeds;
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
     * Replays a single transaction against an account for position recalculation.
     * <p>
     * POSITION-ONLY CONTRACT: This method intentionally replays ONLY position-affecting transaction types (BUY, SELL, SPLIT, DIVIDEND_REINVEST).
     * <p>
     * Cash events (DEPOSIT, WITHDRAWAL, FEE, DIVIDEND, etc.) are deliberately
     * excluded. PositionRecalculationService calls account.clearPosition() then
     * replays transactions for a specific symbol — cash balance is already correct
     * in the DB and must NOT be touched here. Replaying cash events would
     * double-count every cash movement.
     * <p>
     * If you ever need full account reconstruction from scratch (e.g., migration),
     * you will need a separate replay path that resets cash to zero first.
     * <p>
     * make sure position.sell() downstream is actually using the proceeds value
     * for realized gain calculation and not discarding it.
     * If that method only uses quantity to reduce the lot and ignores the
     * proceeds amount, the bug is dormant but still wrong.
     * it'll surface the moment you wire up tax lot reporting
     */
    @Override
    public void replayTransaction(Account account, Transaction tx) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(tx, "Transaction cannot be null");

        if (tx.isExcluded()) {
            return;
        }

        // HARD GUARD: replayTransaction is a POSITION-ONLY contract.
        // If a cash-affecting transaction reaches this method, it is a caller
        // error — not a silent no-op. Failing loudly here surfaces misuse
        // during development and prevents silent cash double-counting in prod.
        // Full-account reconstruction requires a separate path that resets cash.
        if (!tx.transactionType().affectsHoldings()) {
            throw new IllegalArgumentException(
                    "replayTransaction is position-only. Transaction type "
                            + tx.transactionType()
                            + " affects cash and must not be replayed through this path. "
                            + "If full-account reconstruction is needed, use replayFullAccount() "
                            + "which resets cash state before replay.");
        }

        switch (tx.transactionType()) {
            case BUY -> {
                AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
                Position current = account.ensurePosition(tx.execution().asset(), type);

                // Fix #1: use execution.grossValue() (qty × price), NOT cashDelta.abs().
                // cashDelta on BUY includes fees: -(grossCost + fees).
                // Position cost basis must be gross-only for correct ACB/FIFO tax calculation.
                // Using cashDelta.abs() would overstate cost basis by the fee amount.
                Money grossCost = tx.execution().grossValue();

                ApplyResult<? extends Position> result = current.buy(
                        tx.execution().quantity(),
                        grossCost,
                        tx.occurredAt().timestamp());
                account.updatePosition(tx.execution().asset(), result.newPosition());
            }
            case SELL -> {
                account.getPosition(tx.execution().asset()).ifPresent(position -> {
                    // SELL cashDelta is net proceeds (positive) — correct for position P&L
                    ApplyResult<? extends Position> result = position.sell(
                            tx.execution().quantity(),
                            tx.execution().grossValue(),
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
            case DIVIDEND_REINVEST -> {
                // No cash movement — just increase position at cost of grossValue
                AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
                Position current = account.ensurePosition(tx.execution().asset(), type);
                Money totalCost = tx.execution().grossValue();
                ApplyResult<? extends Position> result = current.buy(
                        tx.execution().quantity(), totalCost, tx.occurredAt().timestamp());
                account.updatePosition(tx.execution().asset(), result.newPosition());
            }
            default -> {
                // All cash-only types (DEPOSIT, WITHDRAWAL, FEE, DIVIDEND, INTEREST,
                // TRANSFER_IN, TRANSFER_OUT, RETURN_OF_CAPITAL, etc.) are intentionally
                // skipped. See method Javadoc for the rationale.
            }
        }
    }

    @Override
    public void replayFullTransaction(Account account, Transaction tx) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(tx, "Transaction cannot be null");

        if (tx.isExcluded()) {
            return;
        }

        switch (tx.transactionType()) {
            // Position-affecting — same as replayTransaction
            case BUY -> {
                AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
                Position current = account.ensurePosition(tx.execution().asset(), type);
                Money grossCost = tx.execution().grossValue();
                ApplyResult<? extends Position> result = current.buy(
                        tx.execution().quantity(), grossCost, tx.occurredAt().timestamp());
                account.updatePosition(tx.execution().asset(), result.newPosition());
            }
            case SELL -> {
                Position current = account.getPosition(tx.execution().asset())
                        .orElseThrow(() -> new IllegalStateException(
                                "No position for " + tx.execution().asset().value() + " during full replay"));
                ApplyResult<? extends Position> result = current.sell(
                        tx.execution().quantity(), tx.execution().grossValue(), tx.occurredAt().timestamp());
                account.updatePosition(tx.execution().asset(), result.newPosition());
                account.deposit(tx.cashDelta(), "REPLAY SELL " + tx.execution().asset().value());
            }
            case SPLIT -> {
                Position current = account.getPosition(tx.execution().asset())
                        .orElseThrow(() -> new IllegalStateException(
                                "No position for " + tx.execution().asset().value() + " during full replay"));
                ApplyResult<? extends Position> result = current.split(tx.split().ratio());
                account.updatePosition(tx.execution().asset(), result.newPosition());
            }
            case DIVIDEND_REINVEST -> {
                AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
                Position current = account.ensurePosition(tx.execution().asset(), type);
                ApplyResult<? extends Position> result = current.buy(
                        tx.execution().quantity(), tx.execution().grossValue(), tx.occurredAt().timestamp());
                account.updatePosition(tx.execution().asset(), result.newPosition());
            }
            // Cash-only — these are the ones replayTransaction skips
            case DEPOSIT -> account.deposit(tx.cashDelta(), "REPLAY DEPOSIT");
            case WITHDRAWAL -> account.withdraw(tx.cashDelta().abs(), "REPLAY WITHDRAWAL");
            case DIVIDEND -> account.deposit(tx.cashDelta(), "REPLAY DIVIDEND");
            case FEE -> account.withdraw(tx.cashDelta().abs(), "REPLAY FEE");
            case INTEREST -> account.deposit(tx.cashDelta(), "REPLAY INTEREST");
            case TRANSFER_IN -> account.deposit(tx.cashDelta(), "REPLAY TRANSFER_IN");
            case TRANSFER_OUT -> account.withdraw(tx.cashDelta().abs(), "REPLAY TRANSFER_OUT");

            default -> throw new IllegalStateException(
                    "Unhandled transaction type in replayFullTransaction: " + tx.transactionType()
                            + ". Update this switch when adding new TransactionTypes.");
        }
    }
}
