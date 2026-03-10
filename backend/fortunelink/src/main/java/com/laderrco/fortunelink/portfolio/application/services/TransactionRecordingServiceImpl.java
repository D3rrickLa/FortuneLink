package com.laderrco.fortunelink.portfolio.application.services;

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
import com.laderrco.fortunelink.portfolio.domain.services.projectors.TransactionApplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

// handle like this Position + Transaction -> Apply Result (PositioNTransactionApplier)
@Service
@RequiredArgsConstructor
public class TransactionRecordingServiceImpl implements TransactionRecordingService {

  @Override
  public Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type,
      Quantity quantity, Price price, List<Fee> fees, String notes, Instant date) {

    validateTradeInputs(account, symbol, quantity, price, notes, date);
    validateIsActive(account);
    validateDate(date, account);

    List<Fee> feeList = fees != null ? fees : List.of();

    // BUY cashImpact = OUT → gross + fees, negated (cash leaves account)
    // Transaction.validateTradeConsistency() enforces this exact formula — it
    // will throw at construction if our math doesn't match.
    Money gross = price.calculateValue(quantity);
    Money totalFees = Fee.totalInAccountCurrency(feeList, account.getAccountCurrency());
    Money cashDelta = gross.add(totalFees).negate();

    Transaction tx = Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(TransactionType.BUY)
        .execution(new TradeExecution(symbol, quantity, price))
        .cashDelta(cashDelta)
        .fees(feeList)
        .notes(notes)
        .occurredAt(TransactionDate.of(date))
        .metadata(TransactionMetadata.manual(type))
        .build();

    applyPositionEffect(account, tx);
    account.withdraw(cashDelta.abs(), "BUY " + symbol.symbol());

    return tx;
  }

  @Override
  public Transaction recordSell(Account account, AssetSymbol symbol, Quantity quantity,
      Price price, List<Fee> fees, String notes, Instant date) {

    validateTradeInputs(account, symbol, quantity, price, notes, date);
    validateIsActive(account);
    validateDate(date, account);

    // Position must exist before we can sell. Grab AssetType from it for metadata.
    Position existingPosition = account.getPosition(symbol)
        .orElseThrow(() -> new IllegalStateException(
            "Cannot sell: no open position for " + symbol.symbol()));

    List<Fee> feeList = fees != null ? fees : List.of();

    // SELL cashImpact = IN → gross - fees (net proceeds, cash enters account)
    Money gross = price.calculateValue(quantity);
    Money totalFees = Fee.totalInAccountCurrency(feeList, account.getAccountCurrency());
    Money cashDelta = gross.subtract(totalFees);

    Transaction tx = Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(TransactionType.SELL)
        .execution(new TradeExecution(symbol, quantity, price))
        .cashDelta(cashDelta)
        .fees(feeList)
        .notes(notes)
        .occurredAt(TransactionDate.of(date))
        .metadata(TransactionMetadata.manual(existingPosition.type()))
        .build();

    // Position first: realized gain is recorded inside applyPositionEffect via
    // ApplyResult.Sale. Cash deposit follows — account state is always consistent
    // after this method returns even if deposit() throws.
    applyPositionEffect(account, tx);
    account.deposit(cashDelta, "SELL " + symbol.symbol());

    return tx;
  }

  @Override
  public Transaction recordDeposit(Account account, Money amount, String notes, Instant date) {
    validateCashInputs(account, amount, notes, date);
    validateIsActive(account);
    validateDate(date, account);

    Transaction tx = Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(TransactionType.DEPOSIT)
        .cashDelta(amount)
        .fees(List.of())
        .notes(notes)
        .occurredAt(TransactionDate.of(date))
        .metadata(TransactionMetadata.manual(AssetType.CASH))
        .build();

    account.deposit(amount, "DEPOSIT");

    return tx;
  }

  @Override
  public Transaction recordWithdrawal(Account account, Money amount, String notes, Instant date) {
    validateCashInputs(account, amount, notes, date);
    validateIsActive(account);
    validateDate(date, account);

    Transaction tx = Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(TransactionType.WITHDRAWAL)
        .cashDelta(amount.negate())
        .fees(List.of())
        .notes(notes)
        .occurredAt(TransactionDate.of(date))
        .metadata(TransactionMetadata.manual(AssetType.CASH))
        .build();

    account.withdraw(amount, "WITHDRAWAL");

    return tx;
  }

  @Override
  public Transaction recordFee(Account account, Money amount, String notes, Instant date) {
    validateCashInputs(account, amount, notes, date);
    validateIsActive(account);
    validateDate(date, account);

    Transaction tx = Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(TransactionType.FEE)
        .cashDelta(amount.negate())
        .fees(List.of())
        .notes(notes)
        .occurredAt(TransactionDate.of(date))
        .metadata(TransactionMetadata.manual(AssetType.CASH))
        .build();

    account.applyFee(amount, "FEE: " + notes);

    return tx;
  }

  @Override
  public Transaction recordInterest(Account account, AssetSymbol symbol, Money amount, String notes, Instant date) {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    validateCashInputs(account, amount, notes, date);
    validateIsActive(account);
    validateDate(date, account);

    Transaction tx = Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(TransactionType.INTEREST)
        .cashDelta(amount)
        .fees(List.of())
        .notes(notes)
        .occurredAt(TransactionDate.of(date))
        .metadata(TransactionMetadata.manual(AssetType.CASH)
            .with(TransactionMetadata.KEY_SYMBOL, symbol.symbol()))
        .build();

    account.deposit(amount, "INTEREST: " + symbol.symbol());

    return tx;
  }

  @Override
  public Transaction recordDividend(Account account, AssetSymbol symbol, Money amount, String notes, Instant date) {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    validateCashInputs(account, amount, notes, date);
    validateIsActive(account);
    validateDate(date, account);

    Transaction tx = Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(TransactionType.DIVIDEND)
        .cashDelta(amount)
        .fees(List.of())
        .notes(notes)
        .occurredAt(TransactionDate.of(date))
        .metadata(TransactionMetadata.manual(AssetType.CASH)
            .with(TransactionMetadata.KEY_SYMBOL, symbol.symbol()))
        .build();

    account.deposit(amount, "DIVIDEND: " + symbol.symbol());

    return tx;
  }

  @Override
  public Transaction recordDividendReinvestment(Account account, AssetSymbol symbol,
      Quantity quantity, Price price, String notes, Instant date) {

    validateTradeInputs(account, symbol, quantity, price, notes, date);
    validateIsActive(account);
    validateDate(date, account);

    AssetType type = account.getPosition(symbol)
        .map(Position::type)
        .orElse(AssetType.STOCK);

    // DRIP cashImpact = NONE → cashDelta must be zero.
    // grossValue on the execution is what TransactionApplier uses for ACB.
    // Do NOT call recordDividend() for the same event — the cash never lands first.
    Transaction tx = Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(TransactionType.DIVIDEND_REINVEST)
        .execution(new TradeExecution(symbol, quantity, price))
        .cashDelta(Money.ZERO(account.getAccountCurrency()))
        .fees(List.of())
        .notes(notes)
        .occurredAt(TransactionDate.of(date))
        .metadata(TransactionMetadata.manual(type))
        .build();

    applyPositionEffect(account, tx);
    // No cash mutation — DRIP is self-contained.

    return tx;
  }

  @Override
  public Transaction recordReturnOfCapital(Account account, AssetSymbol symbol,
      Quantity quantity, Price price, String notes, Instant date) {

    validateTradeInputs(account, symbol, quantity, price, notes, date);
    validateIsActive(account);
    validateDate(date, account);

    Position existingPosition = account.getPosition(symbol)
        .orElseThrow(() -> new IllegalStateException(
            "Cannot apply ROC: no open position for " + symbol.symbol()));

    // ROC cashImpact = IN → gross distribution paid to the investor.
    // No fees on ROC distributions.
    Money cashDelta = price.calculateValue(quantity);

    Transaction tx = Transaction.builder()
        .transactionId(TransactionId.newId())
        .accountId(account.getAccountId())
        .transactionType(TransactionType.RETURN_OF_CAPITAL)
        .execution(new TradeExecution(symbol, quantity, price))
        .cashDelta(cashDelta)
        .fees(List.of())
        .notes(notes)
        .occurredAt(TransactionDate.of(date))
        .metadata(TransactionMetadata.manual(existingPosition.type()))
        .build();

    // Position first: applyReturnOfCapital() reduces ACB on the position.
    // Cash deposit follows.
    applyPositionEffect(account, tx);
    account.deposit(cashDelta, "RETURN OF CAPITAL: " + symbol.symbol());

    return tx;
  }

  @Override
  public Transaction recordTransferIn(Account account, Money amount, String notes, Instant date) {
    // Awaiting design decision on inter-account transfers - Bug 6.
    throw new UnsupportedOperationException("recordTransferIn not yet implemented");
  }

  @Override
  public Transaction recordTransferOut(Account account, Money amount, String notes, Instant date) {
    throw new UnsupportedOperationException("recordTransferOut not yet implemented");
  }

  /**
   * Position-only replay. Cash state is NOT touched.
   *
   * Passing a non-holdings type is a programming error — throws immediately so it
   * surfaces in tests rather than silently corrupting state.
   */
  @Override
  public void replayTransaction(Account account, Transaction tx) {
    if (tx.isExcluded())
      return;

    if (!tx.transactionType().affectsHoldings()) {
      throw new IllegalArgumentException(
          "replayTransaction() is position-only. TransactionType."
              + tx.transactionType() + " does not affect holdings. "
              + "Use replayFullTransaction() for cash events.");
    }

    applyPositionEffect(account, tx);
  }

  /**
   * Full replay - position AND cash. Caller MUST reset both to zero first.
   */
  @Override
  public void replayFullTransaction(Account account, Transaction tx) {
    if (tx.isExcluded())
      return;

    if (tx.transactionType().affectsHoldings()) {
      applyPositionEffect(account, tx);
    }

    // allowNegative=true on withdrawals: historical ordering may temporarily push
    // cash negative before a subsequent deposit corrects it. Expected, not bad
    // data.
    switch (tx.transactionType().cashImpact()) {
      case IN -> account.deposit(tx.cashDelta(), "REPLAY " + tx.transactionType());
      case OUT -> account.withdraw(tx.cashDelta().abs(), "REPLAY " + tx.transactionType(), true);
      case NONE -> {
        /* DRIP, SPLIT — no cash effect */ }
    }
  }

  private void applyPositionEffect(Account account, Transaction tx) {
    if (tx.execution() == null)
      return;

    AssetSymbol symbol = tx.execution().asset();
    AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;

    Position current;

    switch (tx.transactionType()) {
      case BUY, DIVIDEND_REINVEST -> current = account.ensurePosition(symbol, type);
      case SELL, RETURN_OF_CAPITAL, SPLIT -> current = requirePosition(account, symbol, tx.transactionType());
      default -> {
        return; // no position effect
      }
    }

    ApplyResult<? extends Position> result = TransactionApplier.apply(current, tx);
    account.updatePosition(symbol, result.newPosition());

    // SELL realized gains
    if (result instanceof ApplyResult.Sale<?> sale) {
      account.recordRealizedGain(
          symbol,
          sale.realizedGainLoss(),
          sale.costBasisSold(),
          tx.occurredAt().timestamp());
    }

    // NEW: ROC excess capital gain
    if (result instanceof ApplyResult.RocAdjustment<?> roc) {
      account.recordRealizedGain(
          symbol,
          roc.excessCapitalGain(),
          Money.ZERO(account.getAccountCurrency()), // cost basis sold = $0.00 per CRA
          tx.occurredAt().timestamp());
    }
  }

  private void validateTradeInputs(Account account, AssetSymbol symbol, Quantity quantity,
      Price price, String notes, Instant date) {
    Objects.requireNonNull(account, "account cannot be null");
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(quantity, "quantity cannot be null");
    Objects.requireNonNull(price, "price cannot be null");
    Objects.requireNonNull(notes, "notes cannot be null");
    Objects.requireNonNull(date, "date cannot be null");
  }

  private void validateCashInputs(Account account, Money amount, String notes, Instant date) {
    Objects.requireNonNull(account, "account cannot be null");
    Objects.requireNonNull(amount, "amount cannot be null");
    Objects.requireNonNull(notes, "notes cannot be null");
    Objects.requireNonNull(date, "date cannot be null");
  }

  private void validateIsActive(Account account) {
    if (!account.isActive()) {
      throw new AccountClosedException(
          "Cannot record transaction on closed account: " + account.getAccountId());
    }
  }

  private void validateDate(Instant date, Account account) {
    if (date.isAfter(Instant.now())) {
      throw new IllegalArgumentException(
          "Transaction date cannot be in the future: " + date);
    }
    if (date.isBefore(account.getCreationDate())) {
      throw new IllegalArgumentException(
          "Transaction date " + date + " predates account creation "
              + account.getCreationDate());
    }
  }

  private Position requirePosition(Account account, AssetSymbol symbol, TransactionType kind) {
    return account.getPosition(symbol)
        .orElseThrow(() -> new IllegalStateException(
            "Replay error: " + kind + " requires existing position for " + symbol));
  }
}
