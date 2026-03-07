package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionFactory;
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
  private final TransactionApplier applier;



  @Override
  public Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type,
      Quantity quantity, Price price, List<Fee> fees, String notes, Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordDeposit(Account account, Money amount, String notes, Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordDividend(Account account, AssetSymbol symbol, Money amount, String notes,
      Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordDividendReinvestment(Account account, AssetSymbol symbol,
      Quantity quantity, Price price, String notes, Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordFee(Account account, Money amount, String notes, Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordInterest(Account account, AssetSymbol symbol, Money amount, String notes,
      Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordReturnOfCapital(Account account, AssetSymbol symbol, Quantity quantity,
      Price price, String notes, Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordSell(Account account, AssetSymbol symbol, Quantity quantity, Price price,
      List<Fee> fees, String notes, Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordTransferIn(Account account, Money amount, String notes, Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordTransferOut(Account account, Money amount, String notes, Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Transaction recordWithdrawal(Account account, Money amount, String notes, Instant date) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void replayTransaction(Account account, Transaction tx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void replayFullTransaction(Account account, Transaction tx) {
    // TODO Auto-generated method stub

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
