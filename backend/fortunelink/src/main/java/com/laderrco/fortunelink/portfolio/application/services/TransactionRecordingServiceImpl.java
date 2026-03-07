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
import com.laderrco.fortunelink.portfolio.domain.services.projectors.PositionTransactionApplier;
import com.laderrco.fortunelink.portfolio.domain.utils.TradeValueResolver;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

// handle llike this Position + Transaction -> Apply Result (PositioNTransactionApplier)
@Service
@RequiredArgsConstructor
public class TransactionRecordingServiceImpl implements TransactionRecordingService {
  private final Logger log = LoggerFactory.getLogger(TransactionRecordingServiceImpl.class);
  private final TradeValueResolver taxResolver;

  @Override
  public Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type,
      Quantity quantity, Price price, List<Fee> fees, String notes, Instant date) {
    validateInputs(account, symbol, quantity, price, notes, date);
    validateDate(date, account);
    validateIsActive(account);

    Currency currency = account.getAccountCurrency();
    List<Fee> feeList = (fees != null) ? fees : List.of();
    Money totalFee = Fee.totalInAccountCurrency(feeList, currency);
    Money grossCost = price.pricePerUnit().multiply(quantity);
    Money totalOutflow = grossCost.add(totalFee);

    // Cash leaves account
    Transaction tx = new Transaction(TransactionId.newId(), account.getAccountId(), TransactionType.BUY,
        new TradeExecution(symbol, quantity, price), null, totalOutflow.negate(), feeList,
        notes, TransactionDate.of(date), null, TransactionMetadata.manual(type));

    Position current = account.ensurePosition(symbol, type);
    ApplyResult<?> result = current.buy(quantity, taxResolver.buyerCost(tx), date);

    account.updatePosition(symbol, result.newPosition());
    account.withdraw(totalOutflow, "BUY " + symbol.value());

    return tx;
  }

  @Override
  public Transaction recordSell(Account account, AssetSymbol symbol, Quantity quantity, Price price,
      List<Fee> fees, String notes, Instant date) {
    validateInputs(account, symbol, quantity, price, notes, date);
    validateDate(date, account);
    validateIsActive(account);

    Currency currency = account.getAccountCurrency();
    List<Fee> feeList = (fees != null) ? fees : List.of();
    Money totalFee = Fee.totalInAccountCurrency(feeList, currency);

    Position current = account.getPosition(symbol)
        .orElseThrow(() -> new NoPositionException(symbol, account.getAccountId()));

    Money grossProceeds = price.pricePerUnit().multiply(quantity.amount());
    Money netProceeds = grossProceeds.subtract(totalFee);

    // Cash enters account
    Transaction tx = new Transaction(TransactionId.newId(), account.getAccountId(),
        TransactionType.SELL, new TradeExecution(symbol, quantity, price), null, netProceeds,
        feeList, notes, TransactionDate.of(date), null, TransactionMetadata.manual(current.type()));

    Money proceeds = taxResolver.sellerProceeds(tx);
    ApplyResult<?> result = current.sell(quantity, proceeds, date);

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
    validateDate(date, account);
    validateIsActive(account);

    account.deposit(amount, "DEPOSIT");
    // cashDelta is positive
    // deposits have no fees
    return new Transaction(TransactionId.newId(), account.getAccountId(), TransactionType.DEPOSIT,
        null, null, amount, List.of(), notes.trim(), TransactionDate.of(date), null,
        TransactionMetadata.manual(AssetType.CASH));
  }

  @Override
  public Transaction recordWithdrawal(Account account, Money amount, String notes, Instant date) {
    validateInputs(account, amount, notes, date);
    validateDate(date, account);
    validateIsActive(account);

    account.withdraw(amount, "WITHDRAWAL");

    // cashDelta is negative — cash leaves
    return new Transaction(TransactionId.newId(), account.getAccountId(),
        TransactionType.WITHDRAWAL, null, null, amount.negate(), List.of(), notes.trim(),
        TransactionDate.of(date), null, TransactionMetadata.manual(AssetType.CASH));
  }

  @Override
  public Transaction recordFee(Account account, Money amount, String notes, Instant date) {
    validateInputs(account, amount, notes, date);
    validateDate(date, account);
    validateIsActive(account);

    account.applyFee(amount, "FEE");

    // cash leaves
    return new Transaction(TransactionId.newId(), account.getAccountId(), TransactionType.FEE, null,
        null, amount.negate(), List.of(), notes.trim(), TransactionDate.of(date), null,
        TransactionMetadata.manual(AssetType.CASH));
  }

  @Override
  public Transaction recordInterest(Account account, AssetSymbol symbol, Money amount, String notes,
      Instant date) {
    Objects.requireNonNull(symbol, "Symbol cannot be null");
    validateInputs(account, amount, notes, date);
    validateDate(date, account);
    validateIsActive(account);

    account.deposit(amount, "INTEREST from " + symbol.value());

    // Resolve asset type from existing position if available, default to STOCK
    AssetType type = account.getPosition(symbol).map(Position::type).orElse(AssetType.STOCK);

    return new Transaction(
        TransactionId.newId(),
        account.getAccountId(),
        TransactionType.INTEREST,
        null,
        null,
        amount,
        List.of(),
        notes.trim(),
        TransactionDate.of(date),
        null,
        TransactionMetadata.manual(type));

  }

  /**
   * Records a dividend payment — credits cash, no position change. The symbol is
   * tracked for tax
   * reporting purposes (taxable income).
   */
  @Override
  public Transaction recordDividend(Account account, AssetSymbol symbol, Money amount, String notes,
      Instant date) {
    Objects.requireNonNull(symbol, "Symbol cannot be null");
    validateInputs(account, amount, notes, date);
    validateDate(date, account);
    validateIsActive(account);

    account.deposit(amount, "DIVIDEND from " + symbol.value());

    // Resolve asset type from existing position if available, default to STOCK
    AssetType type = account.getPosition(symbol).map(Position::type).orElse(AssetType.STOCK);

    // DIVIDEND does not requiresExecution per TransactionType enum
    // cash comes in
    return new Transaction(TransactionId.newId(), account.getAccountId(), TransactionType.DIVIDEND,
        null, null, amount, List.of(), notes.trim(), TransactionDate.of(date), null,
        TransactionMetadata.manual(type));
  }

  /**
   * Records a dividend reinvestment (DRIP): - No cash movement (NONE impact per
   * TransactionType
   * enum) - Increases position using the dividend proceeds
   */
  @Override
  public Transaction recordDividendReinvestment(Account account, AssetSymbol symbol,
      Quantity quantity, Price price, String notes, Instant date) {
    validateInputs(account, symbol, quantity, price, notes, date);
    validateDate(date, account);
    validateIsActive(account);

    Money totalCost = price.pricePerUnit().multiply(quantity.amount());
    AssetType type = account.getPosition(symbol).map(Position::type).orElse(AssetType.STOCK);

    Position current = account.ensurePosition(symbol, type);
    ApplyResult<?> result = current.buy(quantity, totalCost, date);
    account.updatePosition(symbol, result.newPosition());

    // DIVIDEND_REINVEST has CashImpact.NONE; cashDelta must be zero
    return new Transaction(TransactionId.newId(), account.getAccountId(),
        TransactionType.DIVIDEND_REINVEST, new TradeExecution(symbol, quantity, price), null,
        Money.ZERO(account.getAccountCurrency()), List.of(), notes.trim(), TransactionDate.of(date),
        null, TransactionMetadata.manual(type));
  }

  @Override
  public Transaction recordReturnOfCapital(Account account, AssetSymbol symbol, Quantity quantity,
      Price distPerUnitPrice, String notes, Instant date) {
    validateInputs(account, symbol, quantity, distPerUnitPrice, notes, date);
    validateDate(date, account);
    validateIsActive(account);

    Money totalDistribution = distPerUnitPrice.calculateValue(quantity);
    AssetType type = account.getPosition(symbol).map(Position::type).orElse(AssetType.STOCK);

    Position current = account.ensurePosition(symbol, type);
    ApplyResult<?> result = current.applyReturnOfCapital(distPerUnitPrice, quantity);
    account.updatePosition(symbol, result.newPosition());
    account.deposit(totalDistribution, "ROC from " + symbol.value());

    // Bug 5 fix: cashDelta = totalDistribution (CashImpact.IN).
    // Previously was Money.ZERO which caused Transaction constructor to throw
    // a cash delta mismatch because the enum was NONE but impl deposited cash.
    return new Transaction(TransactionId.newId(), account.getAccountId(),
        TransactionType.RETURN_OF_CAPITAL, new TradeExecution(symbol, quantity, distPerUnitPrice),
        null, totalDistribution, List.of(), notes, TransactionDate.of(date), null,
        TransactionMetadata.manual(type));
  }

  @Override
  public Transaction recordTransferIn(Account account, Money amount, String notes, Instant date) {
    throw new UnsupportedOperationException("recordTransferIn not yet implemented — Bug 6");
  }

  @Override
  public Transaction recordTransferOut(Account account, Money amount, String notes, Instant date) {
    throw new UnsupportedOperationException("recordTransferOut not yet implemented — Bug 6");
  }

  @Override
  public void replayTransaction(Account account, Transaction tx) {
    if (shouldSkip(account, tx)) {
      return;
    }

    if (!tx.transactionType().affectsHoldings()) {
      throw new IllegalArgumentException("replayTransaction is position-only.");
    }

    AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
    Position current = account.ensurePosition(tx.execution().asset(), type);

    ApplyResult<? extends Position> result = PositionTransactionApplier.apply(current, tx);

    account.updatePosition(tx.execution().asset(), result.newPosition());

    if (result instanceof ApplyResult.Sale<?> sale) {
      account.recordRealizedGain(
          tx.execution().asset(),
          sale.realizedGainLoss(),
          sale.costBasisSold(),
          tx.occurredAt().timestamp());
    }
  }

  // this is for the scenario of bulk import - account imports from scratch
  @Override
  public void replayFullTransaction(Account account, Transaction tx) {
    if (shouldSkip(account, tx)) {
      return;
    }

    switch (tx.transactionType()) {
      case BUY -> {
        // AssetType type = tx.metadata() != null ? tx.metadata().assetType() :
        // AssetType.STOCK;
        // Position current = account.ensurePosition(tx.execution().asset(), type);
        // Money totalCostIncludingFees = taxResolver.buyerCost(tx);

        // ApplyResult<?> result = current.buy(tx.execution().quantity(),
        // totalCostIncludingFees,
        // tx.occurredAt().timestamp());

        // account.updatePosition(tx.execution().asset(), result.newPosition());
        // account.withdraw(totalCostIncludingFees, "REPLAY BUY", true);
        AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
        Position current = account.ensurePosition(tx.execution().asset(), type);
        ApplyResult<?> result = PositionTransactionApplier.apply(current, tx);
      }
      case SELL -> {
        Position current = account.getPosition(tx.execution().asset())
            .orElseThrow(() -> new IllegalStateException(String
                .format("No position for %s during full replay", tx.execution().asset().value())));

        Money proceeds = taxResolver.sellerProceeds(tx);
        ApplyResult<?> result = current.sell(tx.execution().quantity(), proceeds, tx.occurredAt().timestamp());

        account.updatePosition(tx.execution().asset(), result.newPosition());
        account.deposit(proceeds, "REPLAY SELL " + tx.execution().asset().value());

        if (result instanceof ApplyResult.Sale<?> sale) {
          account.recordRealizedGain(tx.execution().asset(), sale.realizedGainLoss(),
              sale.costBasisSold(), tx.occurredAt().timestamp());
        }
      }
      case SPLIT -> {
        account.getPosition(tx.execution().asset()).ifPresentOrElse(position -> {
          ApplyResult<? extends Position> result = position.split(tx.split().ratio());
          account.updatePosition(tx.execution().asset(), result.newPosition());
        }, () -> log.warn("Full Replay: SPLIT for {} but no active position found. Skipping.",
            tx.execution().asset().value()));
      }
      case DIVIDEND_REINVEST -> {
        AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;
        Position current = account.ensurePosition(tx.execution().asset(), type);
        Money cost = tx.execution().grossValue();
        ApplyResult<? extends Position> result = current.buy(tx.execution().quantity(), cost,
            tx.occurredAt().timestamp());
        account.updatePosition(tx.execution().asset(), result.newPosition());
        // Consume cash — mirrors the dividend proceeds that funded this reinvestment.
        // allowNegative=true handles broker-native DRIP with no paired DIVIDEND tx.
        account.withdraw(cost, "REPLAY DIVIDEND_REINVEST", true);
      }
      case RETURN_OF_CAPITAL -> {
        // Bug 5 fix: was previously a no-op in the NONE/OTHER/REINVESTED_CAPITAL_GAIN
        // block. ROC must (a) reduce ACB on the position and (b) deposit the
        // distribution into cash.
        account.getPosition(tx.execution().asset()).ifPresent(position -> {
          ApplyResult<? extends Position> result = position
              .applyReturnOfCapital(tx.execution().pricePerUnit(), tx.execution().quantity());
          account.updatePosition(tx.execution().asset(), result.newPosition());
        });
        account.deposit(tx.cashDelta(),
            "REPLAY RETURN_OF_CAPITAL " + tx.execution().asset().value());
      }
      case DEPOSIT, INTEREST, TRANSFER_IN, DIVIDEND -> account.deposit(tx.cashDelta(),
          "REPLAY " + tx.transactionType());
      case WITHDRAWAL, FEE, TRANSFER_OUT -> account.withdraw(tx.cashDelta().abs(),
          "REPLAY " + tx.transactionType());
      case OTHER, REINVESTED_CAPITAL_GAIN -> {
        // Intentional no-ops: no position or cash state to reconstruct.
        // REINVESTED_CAPITAL_GAIN is an unimplemented stub (Bug 8 / tech debt).
      }

      default -> throw new IllegalStateException(
          "Unhandled transaction type in replayFullTransaction: " + tx.transactionType()
              + ". Update this switch when adding new TransactionTypes.");
    }
  }

  private boolean shouldSkip(Account account, Transaction tx) {
    Objects.requireNonNull(account, "Account cannot be null");
    Objects.requireNonNull(tx, "Transaction cannot be null");

    return tx.isExcluded();
  }

  private void validateInputs(Account account, AssetSymbol symbol, Quantity quantity, Price price,
      String notes, Instant date) {
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

  private void validateDate(Instant date, Account account) {
    if (date.isAfter(Instant.now())) {
      throw new IllegalArgumentException("Transaction date cannot be in the future: " + date);
    }
    if (date.isBefore(account.getCreationDate())) {
      throw new IllegalArgumentException("Transaction date predates account creation: " + date);
    }
  }
}
