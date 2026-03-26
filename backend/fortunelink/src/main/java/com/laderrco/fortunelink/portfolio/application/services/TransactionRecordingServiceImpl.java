package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;
import com.laderrco.fortunelink.portfolio.domain.services.projectors.TransactionApplier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionRecordingServiceImpl implements TransactionRecordingService {
  private static final String REASON_BUY = "BUY ";
  private static final String REASON_SELL = "SELL ";
  private static final String REASON_DEPOSIT = "DEPOSIT";
  private static final String REASON_WITHDRAWAL = "WITHDRAWAL";
  private static final String REASON_FEE = "FEE: ";
  private static final String REASON_INTEREST = "INTEREST: ";
  private static final String REASON_DIVIDEND = "DIVIDEND: ";
  private static final String REASON_ROC = "RETURN OF CAPITAL: ";

  @Override
  public Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type,
      Quantity quantity, Price price, List<Fee> fees, String notes, Instant date) {
    validateIsActive(account);
    validateTradeInputs(account, symbol, quantity, price, notes, date);
    validateTransactionDate(date, account);

    List<Fee> feeList = fees != null ? fees : List.of();
    Money gross = price.calculateValue(quantity);
    Money totalFees = Fee.totalInAccountCurrency(feeList, account.getAccountCurrency());
    Money cashRequired = gross.add(totalFees);

    if (!account.hasSufficientCash(cashRequired)) {
      throw new InsufficientFundsException(
          String.format("Insufficient cash for buy. Required: %s, Available: %s", cashRequired,
              account.getCashBalance()));
    }

    Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(TransactionType.BUY)
        .execution(new TradeExecution(symbol, quantity, price)).cashDelta(cashRequired.negate())
        .fees(feeList).notes(notes).occurredAt(date).metadata(TransactionMetadata.manual(type))
        .build();

    applyPositionEffect(account, tx);
    account.withdraw(cashRequired, REASON_BUY + symbol, false);
    return tx;
  }

  @Override
  public Transaction recordDeposit(Account account, Money amount, String notes, Instant date) {
    validateIsActive(account);
    validateCashInputs(account, amount, notes, date);
    validateTransactionDate(date, account);

    Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(TransactionType.DEPOSIT)
        .cashDelta(amount).fees(List.of()).notes(notes).occurredAt(date)
        .metadata(TransactionMetadata.manual(AssetType.CASH)).build();

    account.deposit(amount, REASON_DEPOSIT);
    return tx;
  }

  @Override
  public Transaction recordDividend(Account account, AssetSymbol symbol, Money amount, String notes,
      Instant date) {
    validateIsActive(account);
    Objects.requireNonNull(symbol, "symbol cannot be null");
    validateCashInputs(account, amount, notes, date);
    validateTransactionDate(date, account);

    Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(TransactionType.DIVIDEND)
        .cashDelta(amount).fees(List.of()).notes(notes).occurredAt(date).metadata(
            TransactionMetadata.manual(AssetType.CASH)
                .with(TransactionMetadata.KEY_SYMBOL, symbol.symbol())).build();

    account.deposit(amount, REASON_DIVIDEND + symbol.symbol());
    return tx;
  }

  @Override
  public Transaction recordDividendReinvestment(Account account, AssetSymbol symbol,
      Quantity quantity, Price price, String notes, Instant date) {
    validateIsActive(account);
    validateTradeInputs(account, symbol, quantity, price, notes, date);
    validateTransactionDate(date, account);

    AssetType type = account.getPosition(symbol).map(Position::type).orElse(AssetType.STOCK);

    Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(TransactionType.DIVIDEND_REINVEST)
        .execution(new TradeExecution(symbol, quantity, price))
        .cashDelta(Money.zero(account.getAccountCurrency())).fees(List.of()).notes(notes)
        .occurredAt(date).metadata(TransactionMetadata.manual(type)).build();

    applyPositionEffect(account, tx);
    return tx;
  }

  @Override
  public Transaction recordFee(Account account, Money amount, String notes, Instant date) {
    validateIsActive(account);
    validateCashInputs(account, amount, notes, date);
    validateTransactionDate(date, account);

    Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(TransactionType.FEE)
        .cashDelta(amount.negate()).fees(List.of()).notes(notes).occurredAt((date))
        .metadata(TransactionMetadata.manual(AssetType.CASH)).build();

    account.withdraw(amount, REASON_FEE + amount.amount().toString(), false);
    return tx;
  }

  @Override
  public Transaction recordInterest(Account account, AssetSymbol symbol, Money amount, String notes,
      Instant date) {
    validateIsActive(account);
    Objects.requireNonNull(symbol, "symbol cannot be null");
    validateCashInputs(account, amount, notes, date);
    validateTransactionDate(date, account);

    Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(TransactionType.INTEREST)
        .cashDelta(amount).fees(List.of()).notes(notes).occurredAt(date).metadata(
            TransactionMetadata.manual(AssetType.CASH)
                .with(TransactionMetadata.KEY_SYMBOL, symbol.symbol())).build();

    account.deposit(amount, REASON_INTEREST + symbol.symbol());
    return tx;
  }

  @Override
  public Transaction recordReturnOfCapital(Account account, AssetSymbol symbol, Quantity quantity,
      Price price, String notes, Instant date) {
    validateIsActive(account);
    validateTradeInputs(account, symbol, quantity, price, notes, date);
    validateTransactionDate(date, account);
    // NOTE: this and Sell have same signatures
    Position existingPosition = account.getPosition(symbol).orElseThrow(
        () -> new IllegalStateException(
            "Cannot apply ROC: no open position for " + symbol.symbol()));

    if (!quantity.equals(existingPosition.totalQuantity())) {
      throw new IllegalArgumentException(
          "ROC quantity must match total held quantity (" + existingPosition.totalQuantity() + ")");
    }

    Money cashDelta = price.calculateValue(quantity);

    Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(TransactionType.RETURN_OF_CAPITAL)
        .execution(new TradeExecution(symbol, quantity, price)).cashDelta(cashDelta).fees(List.of())
        .notes(notes).occurredAt(date).metadata(TransactionMetadata.manual(existingPosition.type()))
        .build();

    applyPositionEffect(account, tx);
    account.deposit(cashDelta, REASON_ROC + symbol.symbol());
    return tx;
  }

  @Override
  public Transaction recordSell(Account account, AssetSymbol symbol, Quantity quantity, Price price,
      List<Fee> fees, String notes, Instant date) {
    validateIsActive(account);
    validateTradeInputs(account, symbol, quantity, price, notes, date);
    validateTransactionDate(date, account);

    Position existingPosition = account.getPosition(symbol).orElseThrow(
        () -> new IllegalStateException("Cannot sell: no open position for " + symbol.symbol()));

    if (quantity.compareTo(existingPosition.totalQuantity()) > 0) {
      throw new InsufficientQuantityException(
          String.format("Cannot sell %s. Position only holds: %s", quantity,
              existingPosition.totalQuantity()));
    }
    List<Fee> feeList = fees != null ? fees : List.of();
    Money gross = price.calculateValue(quantity);
    Money totalFees = Fee.totalInAccountCurrency(feeList, account.getAccountCurrency());
    Money cashDelta = gross.subtract(totalFees);

    Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(TransactionType.SELL)
        .execution(new TradeExecution(symbol, quantity, price)).cashDelta(cashDelta).fees(feeList)
        .notes(notes).occurredAt(date).metadata(TransactionMetadata.manual(existingPosition.type()))
        .build();

    applyPositionEffect(account, tx);
    account.deposit(cashDelta, REASON_SELL + symbol.symbol());
    return tx;
  }

  @Override
  public Transaction recordTransferIn(Account account, Money amount, String notes, Instant date) {
    throw new UnsupportedOperationException("recordTransferIn not yet implemented");
  }

  @Override
  public Transaction recordTransferOut(Account account, Money amount, String notes, Instant date) {
    throw new UnsupportedOperationException("recordTransferOut not yet implemented");
  }

  @Override
  public Transaction recordWithdrawal(Account account, Money amount, String notes, Instant date) {
    validateIsActive(account);
    validateCashInputs(account, amount, notes, date);
    validateTransactionDate(date, account);

    Transaction tx = Transaction.builder().transactionId(TransactionId.newId())
        .accountId(account.getAccountId()).transactionType(TransactionType.WITHDRAWAL)
        .cashDelta(amount.negate()).fees(List.of()).notes(notes).occurredAt(date)
        .metadata(TransactionMetadata.manual(AssetType.CASH)).build();

    account.withdraw(amount, REASON_WITHDRAWAL, false);
    return tx;
  }

  @Override
  public void replayFullTransaction(Account account, List<Transaction> history) {
    account.beginReplay();
    try {
      if (history != null) {
        for (Transaction tx : history) {
          if (tx.isExcluded()) {
            continue;
          }
          executeReplayStep(account, tx);
        }
      }
    } catch (Exception e) {
      throw e;
    }
    account.endReplay();
  }

  @Override
  public void replayTransaction(Account account, Transaction tx) {
    if (tx.isExcluded()) {
      return;
    }

    if (account.isInReplayMode()) {
      throw new IllegalStateException(
          "Cannot do partial replay on account currently in full replay mode.");
    }

    if (!tx.transactionType().affectsHoldings()) {
      throw new IllegalArgumentException(
          "replayTransaction() is position-only. Use replayFullTransaction().");
    }

    applyPositionEffect(account, tx);
  }

  private void applyPositionEffect(Account account, Transaction tx) {
    if (tx.execution() == null || !tx.transactionType().affectsHoldings()) {
      return;
    }

    AssetSymbol symbol = tx.execution().asset();
    AssetType type = tx.metadata() != null ? tx.metadata().assetType() : AssetType.STOCK;

    if (tx.transactionType() == TransactionType.BUY
        || tx.transactionType() == TransactionType.DIVIDEND_REINVEST) {
      account.ensurePosition(symbol, type);
    }

    Position current = account.getPosition(symbol).orElseThrow(
        () -> new IllegalStateException(tx.transactionType() + " requires position for " + symbol));

    ApplyResult<? extends Position> result = TransactionApplier.apply(current, tx);
    account.applyPositionResult(symbol, result.newPosition());

    if (result instanceof ApplyResult.Sale<?> sale) {
      account.recordRealizedGain(symbol, sale.realizedGainLoss(), sale.costBasisSold(),
          tx.occurredAt());
    } else if (result instanceof ApplyResult.RocAdjustment<?> roc) {
      account.recordRealizedGain(symbol, roc.excessCapitalGain(),
          Money.zero(account.getAccountCurrency()), tx.occurredAt());
    }
  }

  private void executeReplayStep(Account account, Transaction tx) {
    // Apply position effects (Buy, Sell, Reinvest, etc.)
    if (tx.transactionType().affectsHoldings()) {
      applyPositionEffect(account, tx);
    }

    // Apply cash effects
    switch (tx.transactionType().cashImpact()) {
      case IN -> account.deposit(tx.cashDelta(), "REPLAY " + tx.transactionType());
      // allowNegative = true is critical for replaying historical sequences
      case OUT -> account.withdraw(tx.cashDelta().abs(), "REPLAY " + tx.transactionType(), true);
      case NONE -> { /* No cash effect for DRIP/Split */ }
    }
  }

  private void validateIsActive(Account account) {
    if (!account.isActive()) {
      throw new AccountClosedException("Account is closed: " + account.getAccountId());
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

  private void validateTransactionDate(Instant date, Account account) {
    List<String> errors = new ArrayList<>();
    ValidationUtils.validateDate(date, account.getCreationDate(), errors);
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(errors.get(0));
    }
  }
}
