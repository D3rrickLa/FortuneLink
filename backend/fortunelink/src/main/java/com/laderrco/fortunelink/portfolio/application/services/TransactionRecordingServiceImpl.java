package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.NoPositionException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
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
import com.laderrco.fortunelink.portfolio.domain.utils.TaxMethodResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 */
@Service
@RequiredArgsConstructor
public class TransactionRecordingServiceImpl implements TransactionRecordingService {

    private final TaxMethodResolver taxResolver;
    private final Logger log = LoggerFactory.getLogger(TransactionRecordingServiceImpl.class);

    @Override
    public Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type, Quantity quantity, Price price,
                                 List<Fee> fees, String notes, Instant date) {
        validateInputs(account, symbol, quantity, price, notes, date);
        validateDate(date);
        validateIsActive(account);

        Currency currency = account.getAccountCurrency();

        List<Fee> feeList = (fees != null) ? fees : List.of();
        Money totalFee = Fee.totalInAccountCurrency(feeList, currency);

        Money grossCost = price.pricePerUnit().multiply(quantity);
        Money totalOutflow = grossCost.add(totalFee);

        Transaction tx = new Transaction(
                TransactionId.newId(),
                account.getAccountId(),
                TransactionType.BUY,
                new TradeExecution(symbol, quantity, price),
                null,
                totalOutflow.negate(), // Cash leaves account
                feeList,
                notes,
                TransactionDate.of(date),
                null,
                TransactionMetadata.manual(type));

        Position current = account.ensurePosition(symbol, type);
        ApplyResult<? extends Position> result = current.buy(quantity, taxResolver.buyerCost(tx), date);

        account.updatePosition(symbol, result.newPosition());
        account.withdraw(totalOutflow, "BUY " + symbol.value());

        return tx;
    }

    @Override
    public Transaction recordSell(Account account, AssetSymbol symbol, Quantity quantity, Price price, List<Fee> fees,
                                  String notes, Instant date) {
        validateInputs(account, symbol, quantity, price, notes, date);
        validateDate(date);
        if (date.isBefore(account.getCreationDate())) {
            throw new IllegalArgumentException("Transaction date predates account creation");
        }
        validateIsActive(account);

        Currency currency = account.getAccountCurrency();

        List<Fee> feeList = (fees != null) ? fees : List.of();
        Money totalFee = Fee.totalInAccountCurrency(feeList, currency);

        Position current = account.getPosition(symbol)
                .orElseThrow(() -> new NoPositionException(symbol, account.getAccountId()));

        Money grossProceeds = price.pricePerUnit().multiply(quantity.amount());
        Money netProceeds = grossProceeds.subtract(totalFee);

        Transaction tx = new Transaction(
                TransactionId.newId(),
                account.getAccountId(),
                TransactionType.SELL,
                new TradeExecution(symbol, quantity, price),
                null,
                netProceeds, // Cash enters account
                feeList,
                notes,
                TransactionDate.of(date),
                null,
                TransactionMetadata.manual(current.type()));

        Money proceeds = taxResolver.sellerProceeds(tx);
        ApplyResult<? extends Position> result = current.sell(quantity, proceeds, date);

        account.updatePosition(symbol, result.newPosition());
        account.deposit(proceeds, "SELL " + symbol.value());

        if (result instanceof ApplyResult.Sale<?> sale) {
            account.recordRealizedGain(symbol, sale.realizedGainLoss(), sale.costBasisSold(), date);
        }

        return tx;
    }

    @Override
    public Transaction recordDeposit(Account account, Money amount, String notes, Instant date) {
        validateInputs(account, amount, notes, date);
        validateDate(date);
        validateIsActive(account);

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
        validateInputs(account, amount, notes, date);
        validateDate(date);
        validateIsActive(account);

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
        validateInputs(account, amount, notes, date);
        validateDate(date);
        validateIsActive(account);

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
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        validateInputs(account, amount, notes, date);
        validateDate(date);
        validateIsActive(account);

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
    public Transaction recordDividendReinvestment(Account account, AssetSymbol symbol, Quantity quantity, Price price,
                                                  String notes, Instant date) {
        validateInputs(account, symbol, quantity, price, notes, date);
        validateDate(date);
        validateIsActive(account);

        Money totalCost = price.pricePerUnit().multiply(quantity.amount());

        // Resolve or create the position
        AssetType type = account.getPosition(symbol)
                .map(Position::type)
                .orElse(AssetType.STOCK);

        Position current = account.ensurePosition(symbol, type);
        ApplyResult<? extends Position> result = current.buy(quantity, totalCost, date);
        account.updatePosition(symbol, result.newPosition());

        // DIVIDEND_REINVEST has CashImpact.NONE; cashDelta must be zero
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

    @Override
    public void replayTransaction(Account account, Transaction tx) {
        if (validateReplay(account, tx)) return;

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

                Money totalCostIncludingFees = taxResolver.buyerCost(tx);

                ApplyResult<? extends Position> result = current.buy(
                        tx.execution().quantity(),
                        totalCostIncludingFees,
                        tx.occurredAt().timestamp());

                account.updatePosition(tx.execution().asset(), result.newPosition());
            }
            case SELL -> {
                Position position = account.getPosition(tx.execution().asset())

                        .orElseThrow(() -> new IllegalStateException(
                                "No position for " + tx.execution().asset().value() + " during position replay. " +
                                        "BUY must precede SELL in replay order."));

                Money proceeds = taxResolver.sellerProceeds(tx);
                ApplyResult<? extends Position> result = position.sell(
                        tx.execution().quantity(),
                        proceeds,
                        tx.occurredAt().timestamp());

                account.updatePosition(tx.execution().asset(), result.newPosition());

                // Capture realized gain - prev discarded, but now preserved
                if (result instanceof ApplyResult.Sale<?> sale) {
                    account.recordRealizedGain(
                            tx.execution().asset(),
                            sale.realizedGainLoss(),
                            sale.costBasisSold(),
                            tx.occurredAt().timestamp());
                }
            }
            case SPLIT -> {
                // A split on a closed position is a no-op, not an error.
                // user may have sold all share before the split date, only apply the split if a position
                // actually exists
                account.getPosition(tx.execution().asset()).ifPresent(position -> {
                    ApplyResult<? extends Position> result = position.split(tx.split().ratio());
                    account.updatePosition(tx.execution().asset(), result.newPosition());
                });
            }
            case DIVIDEND_REINVEST -> {
                // No cash movement, just increase position at cost of grossValue
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
                // skipped.
            }
        }
    }

    // this is for the scenario of bulk import - account imports from scratch
    @Override
    public void replayFullTransaction(Account account, Transaction tx) {
        if (validateReplay(account, tx)) return;

        switch (tx.transactionType()) {
            case BUY -> {
                AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
                Position current = account.ensurePosition(tx.execution().asset(), type);

                Money totalCostIncludingFees = taxResolver.buyerCost(tx);

                ApplyResult<? extends Position> result = current.buy(
                        tx.execution().quantity(),
                        totalCostIncludingFees,
                        tx.occurredAt().timestamp());

                account.updatePosition(tx.execution().asset(), result.newPosition());
                account.withdraw(totalCostIncludingFees, "REPLAY BUY", true);
            }
            case SELL -> {
                Position current = account.getPosition(tx.execution().asset())
                        .orElseThrow(
                                () -> new IllegalStateException(String.format("No position for %s during full replay",
                                        tx.execution().asset().value())));

                Money proceeds = taxResolver.sellerProceeds(tx);
                ApplyResult<? extends Position> result = current.sell(
                        tx.execution().quantity(),
                        proceeds,
                        tx.occurredAt().timestamp());

                account.updatePosition(tx.execution().asset(), result.newPosition());
                account.deposit(proceeds, "REPLAY SELL " + tx.execution().asset().value());

                if (result instanceof ApplyResult.Sale<?> sale) {
                    account.recordRealizedGain(
                            tx.execution().asset(),
                            sale.realizedGainLoss(),
                            sale.costBasisSold(),
                            tx.occurredAt().timestamp());
                }
            }
            case SPLIT -> {
                account.getPosition(tx.execution().asset()).ifPresentOrElse(
                        position -> {
                            ApplyResult<? extends Position> result = position.split(tx.split().ratio());
                            account.updatePosition(tx.execution().asset(), result.newPosition());
                        },
                        () -> log.warn("Full Replay: Received SPLIT for {} but no active position found. Skipping.",
                                tx.execution().asset().value())
                );
            }
            case DIVIDEND_REINVEST -> {
                AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
                Position current = account.ensurePosition(tx.execution().asset(), type);
                Money cost = tx.execution().grossValue();
                ApplyResult<? extends Position> result = current.buy(
                        tx.execution().quantity(), cost, tx.occurredAt().timestamp());
                account.updatePosition(tx.execution().asset(), result.newPosition());
                // Consume cash - mirrors the dividend proceeds that funded this reinvestment.
                // If a paired DIVIDEND transaction deposited the cash, this balances it out.
                // If no paired DIVIDEND exists (broker-native DRIP), allowNegative=true prevents crash.
                account.withdraw(cost, "REPLAY DIVIDEND_REINVEST", true);
            }
            // Cash-only types (DEPOSIT, FEE, etc.) correctly move cash already
            case DEPOSIT, INTEREST, TRANSFER_IN, DIVIDEND ->
                    account.deposit(tx.cashDelta(), "REPLAY " + tx.transactionType());
            case WITHDRAWAL, FEE, TRANSFER_OUT ->
                    account.withdraw(tx.cashDelta().abs(), "REPLAY " + tx.transactionType());
            case RETURN_OF_CAPITAL, OTHER, REINVESTED_CAPITAL_GAIN -> {
                // CashImpact.NONE and affectsHolding=false -> no-op during full replay, intentional
                // These are info/tax records only; no pos or cash state to reconstruct
            }

            default -> throw new IllegalStateException(
                    "Unhandled transaction type in replayFullTransaction: " + tx.transactionType()
                            + ". Update this switch when adding new TransactionTypes.");
        }
    }

    private static boolean validateReplay(Account account, Transaction tx) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(tx, "Transaction cannot be null");

        if (tx.isExcluded()) {
            return true;
        }
        return false;
    }

    private void validateInputs(Account account, AssetSymbol symbol, Quantity quantity, Price price, String notes,
                                Instant date) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(symbol, "Symbol cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");
    }

    private void validateInputs(Account account, Money amount, String notes, Instant date) {
        Objects.requireNonNull(account, "Account cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(notes, "Notes cannot be null");
        Objects.requireNonNull(date, "Date cannot be null");
    }

    private void validateIsActive(Account account) {
        if (!account.isActive()) {
            throw new AccountClosedException(
                    "Cannot record transaction on a closed account: " + account.getAccountId());
        }
    }

    private void validateDate(Instant date) {
        if (date.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Transaction date cannot be in the future: " + date);
        }
    }
}
