package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.HealthStatus;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// NOTE: The variables AccountId, Currency, PositionStrategy, creationDate, and PositionBook
// were initially private final, but now just private, still effectively final because no setters
// this was done becuase of the new reconstitution method 
public class Account {
  private AccountId accountId;
  private Currency accountCurrency;
  private PositionStrategy positionStrategy;
  private Instant creationDate;
  private PositionBook positionBook;
  private String name;
  private AccountType accountType;
  private HealthStatus healthStatus;
  private AccountLifecycleState state;
  private Instant closeDate;
  private Instant lastUpdatedOn;
  private Money cashBalance;
  private List<RealizedGainRecord> realizedGains;

  // JPA hydration constructor only. Fields populated by persistence layer via
  // reflection
  protected Account() {
    this.accountId = null;
    this.accountCurrency = null;
    this.positionStrategy = null;
    this.creationDate = null;
    this.positionBook = new PositionBook(Map.of(), null, null);
    this.realizedGains = new ArrayList<>();
  }

  public Account(AccountId accountId, String name, AccountType accountType,
      Currency accountCurrency, PositionStrategy positionStrategy) {
    this.accountId = notNull(accountId, "accountId");
    this.accountCurrency = notNull(accountCurrency, "accountCurrency");
    this.positionStrategy = notNull(positionStrategy, "positionStrategy");
    this.accountType = notNull(accountType, "accountType");

    updateName(name); // Validates and trims

    this.healthStatus = HealthStatus.HEALTHY;
    this.state = AccountLifecycleState.ACTIVE;
    this.cashBalance = Money.zero(accountCurrency);
    this.positionBook = new PositionBook(accountCurrency, positionStrategy);
    this.realizedGains = new ArrayList<>();
    this.creationDate = Instant.now();
    this.lastUpdatedOn = Instant.now();
  }

  public static Account reconstitute(AccountId accountId, String name, AccountType accountType,
      Currency accountCurrency, PositionStrategy positionStrategy, HealthStatus healthStatus,
      AccountLifecycleState state, Instant closeDate, Instant creationDate, Instant lastUpdatedOn,
      Money cashBalance, Map<AssetSymbol, Position> positions, // from PositionJpaEntity rows
      List<RealizedGainRecord> realizedGains) { // from RealizedGainJpaEntity rows

    Account account = new Account(); // uses the protected no-arg JPA ctor

    // Reflective setters are fragile across refactors; we use package-private
    // field assignment via a dedicated internal init method instead.
    account.initFromPersistence(accountId, name, accountType, accountCurrency, positionStrategy,
        healthStatus, state, closeDate, creationDate, lastUpdatedOn, cashBalance, positions,
        realizedGains);

    return account;
  }

  // --- Cash Operations ---

  void initFromPersistence(AccountId accountId, String name, AccountType accountType,
      Currency accountCurrency, PositionStrategy positionStrategy, HealthStatus healthStatus,
      AccountLifecycleState state, Instant closeDate, Instant creationDate, Instant lastUpdatedOn,
      Money cashBalance, Map<AssetSymbol, Position> positions,
      List<RealizedGainRecord> realizedGains) {

    this.accountId = notNull(accountId, "accountId");
    this.accountCurrency = notNull(accountCurrency, "accountCurrency");
    this.positionStrategy = notNull(positionStrategy, "positionStrategy");
    this.creationDate = notNull(creationDate, "creationDate");

    this.name = name;
    this.accountType = accountType;
    this.healthStatus = healthStatus;
    this.state = state;
    this.closeDate = closeDate;
    this.lastUpdatedOn = lastUpdatedOn;
    this.cashBalance = cashBalance;
    this.realizedGains = new ArrayList<>(realizedGains);

    // PositionBook has a package-private constructor that accepts an existing map.
    // Both Account and PositionBook are in the same package so this is legal.
    this.positionBook = new PositionBook(positions, accountCurrency, positionStrategy);
  }

  public void deposit(Money amount, String reason) {
    requireActive();
    validateCurrency(amount);
    validateReason(reason);

    if (!amount.isPositive()) {
      throw new IllegalArgumentException("Deposit amount must be positive");
    }

    cashBalance = cashBalance.add(amount);
    touch();
  }

  public void withdraw(Money amount, String reason, boolean allowNegative) {
    requireActive();
    validateCurrency(amount);
    validateReason(reason);

    if (!amount.isPositive()) {
      throw new IllegalArgumentException("Withdrawal amount must be positive");
    }

    if (!allowNegative && cashBalance.isLessThan(amount)) {
      throw new InsufficientFundsException(
          "Insufficient funds: required " + amount + ", available " + cashBalance);
    }

    cashBalance = cashBalance.subtract(amount);
    touch();
  }

  public void applyFee(Money feeAmount, String description) {
    withdraw(feeAmount, description, false);
  }

  // --- Position Management ---

  public boolean hasSufficientCash(Money requiredAmount) {
    validateCurrency(requiredAmount);
    return !cashBalance.isLessThan(requiredAmount);
  }

  public void ensurePosition(AssetSymbol symbol, AssetType assetType) {
    positionBook.ensurePosition(symbol, assetType);
  }

  public void applyPositionResult(AssetSymbol symbol, Position updated) {
    requireActive();
    positionBook.applyResult(symbol, updated);
    touch();
  }

  public Optional<Position> getPosition(AssetSymbol symbol) {
    return positionBook.get(symbol);
  }

  public boolean hasPosition(AssetSymbol symbol) {
    return positionBook.has(symbol);
  }

  public int getPositionCount() {
    return positionBook.size();
  }

  // --- Gain Management ---

  public Collection<Map.Entry<AssetSymbol, Position>> getPositionEntries() {
    return positionBook.entries();
  }

  public void recordRealizedGain(AssetSymbol symbol, Money gainLoss, Money costBasisSold,
      Instant at) {
    requireActive();
    notNull(symbol, "symbol");
    notNull(gainLoss, "gainLoss");
    notNull(costBasisSold, "costBasisSold");
    notNull(at, "at");

    realizedGains.add(RealizedGainRecord.of(symbol, gainLoss, costBasisSold, at));
    touch();
  }

  public List<RealizedGainRecord> getRealizedGains() {
    return Collections.unmodifiableList(realizedGains);
  }

  public List<RealizedGainRecord> getRealizedGainsFor(AssetSymbol symbol) {
    notNull(symbol, "symbol");
    return realizedGains.stream().filter(r -> r.symbol().equals(symbol)).toList();
  }

  // --- Lifecycle Transitions ---

  public Money getTotalRealizedGainLoss() {
    return realizedGains.stream().map(RealizedGainRecord::realizedGainLoss)
        .reduce(Money.zero(accountCurrency), Money::add);
  }

  public void beginReplay() {
    if (this.state == AccountLifecycleState.CLOSED) {
      throw new IllegalStateException("Cannot replay a closed account");
    }
    if (this.state == AccountLifecycleState.REPLAYING) {
      throw new IllegalStateException("Account is already in replay mode");
    }

    this.state = AccountLifecycleState.REPLAYING;
    this.cashBalance = Money.zero(this.accountCurrency);
    this.positionBook.clearAll();
    this.realizedGains = new ArrayList<>();
  }

  public void endReplay() {
    if (this.state != AccountLifecycleState.REPLAYING) {
      throw new IllegalStateException("Account is not in replay mode");
    }
    this.state = AccountLifecycleState.ACTIVE;
  }

  void close() {
    if (this.state == AccountLifecycleState.REPLAYING) {
      throw new IllegalStateException("Cannot close account during replay");
    }
    requireActive();

    if (!positionBook.isEmpty()) {
      throw new IllegalStateException("Cannot close account with open positions");
    }
    if (cashBalance.isPositive()) {
      throw new IllegalStateException("Cannot close account with cash balance");
    }

    this.state = AccountLifecycleState.CLOSED;
    this.closeDate = Instant.now();
    touch();
  }

  // --- Maintenance & Recalculation (Internal Use) ---

  void reopen() {
    if (this.state != AccountLifecycleState.CLOSED) {
      throw new IllegalStateException("Can only reopen a closed account");
    }
    this.state = AccountLifecycleState.ACTIVE;
    this.closeDate = null;
    touch();
  }

  public void clearPositionForRecalculation(AssetSymbol symbol) {
    positionBook.clearSymbol(symbol);
    touch();
  }

  // --- Metadata & Health Updates ---

  public void clearRealizedGainsForSymbol(AssetSymbol symbol) {
    notNull(symbol, "symbol");
    realizedGains.removeIf(g -> g.symbol().equals(symbol));
    touch();
  }

  public void updateName(String newName) {
    if (newName == null || newName.trim().isEmpty()) {
      throw new DomainArgumentException("Account name cannot be empty");
    }
    this.name = newName.trim();
    touch();
  }

  public void markStale() {
    this.healthStatus = HealthStatus.STALE;
  }

  public void restoreHealth() {
    this.healthStatus = HealthStatus.HEALTHY;
  }

  // --- Getters ---
  public AccountId getAccountId() {
    return accountId;
  }

  public Currency getAccountCurrency() {
    return accountCurrency;
  }

  public PositionStrategy getPositionStrategy() {
    return positionStrategy;
  }

  public Instant getCreationDate() {
    return creationDate;
  }

  PositionBook getPositionBook() {
    return positionBook;
  }

  public String getName() {
    return name;
  }

  public AccountType getAccountType() {
    return accountType;
  }

  public HealthStatus getHealthStatus() {
    return healthStatus;
  }

  public AccountLifecycleState getState() {
    return state;
  }

  public Instant getCloseDate() {
    return closeDate;
  }

  public Instant getLastUpdatedOn() {
    return lastUpdatedOn;
  }

  // --- Status Helpers ---

  public Money getCashBalance() {
    return cashBalance;
  }

  public boolean isActive() {
    return this.state != AccountLifecycleState.CLOSED;
  }

  public boolean isInReplayMode() {
    return this.state == AccountLifecycleState.REPLAYING;
  }

  public boolean isStale() {
    return this.healthStatus == HealthStatus.STALE;
  }

  private void requireActive() {
    if (!isActive()) {
      throw new AccountClosedException("Account " + accountId + " is closed");
    }
  }

  private void validateCurrency(Money amount) {
    if (!amount.currency().equals(accountCurrency)) {
      throw new CurrencyMismatchException(accountCurrency, amount.currency());
    }
  }

  private void validateReason(String reason) {
    if (reason == null || reason.trim().isEmpty()) {
      throw new IllegalArgumentException("Reason/description cannot be empty");
    }
  }

  private void touch() {
    this.lastUpdatedOn = Instant.now();
  }
}