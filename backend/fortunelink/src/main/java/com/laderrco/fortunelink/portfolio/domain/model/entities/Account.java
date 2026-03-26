package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.HealthStatus;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;

// presents and maintains current state
@Getter
public class Account {
  private final AccountId accountId;
  private final Currency accountCurrency;
  private final PositionStrategy positionStrategy;
  private final Instant creationDate;
  private AccountType accountType;
  private String name;
  private HealthStatus healthStatus;
  private Money cashBalance;
  private final Map<AssetSymbol, Position> positions;
  private List<RealizedGainRecord> realizedGains;
  private LifecycleState state;
  private Instant closeDate;
  private Instant lastUpdatedOn;

  /**
   * JPA-only constructor. Object is in a partially invalid state until Hibernate fully hydrates it
   * — do NOT call business methods on a proxy before that happens.
   * <p>
   * positions and cashBalance are initialized to safe empty defaults so that defensive calls like
   * getPositionEntries() and getCashBalance() on a partially-hydrated proxy don't NPE. All other
   * fields remain null and are set by Hibernate via field injection.
   * <p>
   * accountCurrency is null here, which means any method that calls validateCurrency() will NPE
   * until hydration completes. This is intentional — it surfaces the problem loudly rather than
   * silently operating on wrong currency. Never pass a JPA proxy into business logic before the
   * owning transaction has loaded the full entity.
   */
  protected Account() {
    this.accountId = null;
    this.positionStrategy = null;
    this.creationDate = null;
    this.accountCurrency = null;
    this.positions = new HashMap<>(); // safe: getPositionEntries() won't NPE
    this.realizedGains = new ArrayList<>();
    this.cashBalance = null; // intentionally null: any arithmetic will
    // fail loudly rather than silently wrong
    this.healthStatus = null;
    this.state = null;
  }

  public Account(AccountId accountId, String name, AccountType accountType,
      Currency accountCurrency, PositionStrategy positionStrategy) {
    notNull(accountId, "accountId");
    notNull(name, "name");
    notNull(accountType, "accountType");
    notNull(accountCurrency, "accountCurrency");
    notNull(positionStrategy, "positionStrategy");

    if (name.trim().isEmpty()) {
      throw new DomainArgumentException("Account name cannot be empty");
    }

    this.accountId = accountId;
    this.name = name.trim();
    this.accountType = accountType;
    this.accountCurrency = accountCurrency;
    this.positionStrategy = positionStrategy;
    this.healthStatus = HealthStatus.HEALTHY;
    this.cashBalance = Money.zero(accountCurrency);
    this.positions = new HashMap<>();
    this.realizedGains = new ArrayList<>();
    this.creationDate = Instant.now();
    this.state = LifecycleState.ACTIVE;
    this.closeDate = null;
    this.lastUpdatedOn = Instant.now();
  }

  // DEPOSIT, WITHDRAWAL, DIVIDEND, INTEREST
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

  public void withdraw(Money amount, String reason) {
    withdraw(amount, reason, false); // Default behavior: stay positive
  }

  public void withdraw(Money amount, String reason, boolean allowNegative) {
    requireActive();
    validateCurrency(amount);
    validateReason(reason);

    if (amount.isNegative()) {
      throw new IllegalArgumentException("Withdrawal amount must be positive");
    }

    // Only check funds if we aren't allowing a temp negative balance
    if (!allowNegative && cashBalance.isLessThan(amount)) {
      throw new InsufficientFundsException(
          "Insufficient funds: required " + amount + ", available " + cashBalance);
    }

    cashBalance = cashBalance.subtract(amount);
    touch();
  }

  public void applyFee(Money feeAmount, String description) {
    requireActive();
    validateCurrency(feeAmount);
    validateReason(description);

    if (!feeAmount.isPositive()) {
      throw new IllegalArgumentException("Fee must be positive");
    }

    if (cashBalance.isLessThan(feeAmount)) {
      throw new InsufficientFundsException(
          "Insufficient cash to cover fee: required " + feeAmount + ", available " + cashBalance);
    }

    cashBalance = cashBalance.subtract(feeAmount);
    touch();
  }

  /**
   * Records the realized gain/loss from a sell event.
   * <p>
   * Called by the replay service immediately after position.sell() so that capital gains history is
   * preserved on the account without requiring a full transaction log replay to reconstruct it.
   * <p>
   * This is the only place RealizedGainRecord entries are created. Do NOT call this for unrealized
   * gains, only on actual sell events.
   */
  public void recordRealizedGain(AssetSymbol symbol, Money realizedGainLoss, Money costBasisSold,
      Instant occurredAt) {
    if (this.state == LifecycleState.CLOSED) {
      throw new AccountClosedException("Account " + accountId + " is closed");
    }
    notNull(symbol, "symbol");
    notNull(realizedGainLoss, "realizedGainLoss");
    notNull(costBasisSold, "costBasisSold");
    notNull(occurredAt, "occurredAt");

    realizedGains.add(new RealizedGainRecord(symbol, realizedGainLoss, costBasisSold, occurredAt));
    touch();
  }

  public void clearRealizedGains(AssetSymbol symbol) {
    notNull(symbol, "symbol");
    this.realizedGains.removeIf(gain -> gain.symbol().equals(symbol));
    touch();
  }

  public void clearAllRealizedGains() {
    this.realizedGains = new ArrayList<>();
    touch();
  }

  // dividend reinvestment + buy + sell
  public void updatePosition(AssetSymbol symbol, Position newPosition) {
    requireActive();
    notNull(symbol, "symbol");
    notNull(newPosition, "newPosition");

    // Validate position belongs to this symbol
    if (!newPosition.symbol().equals(symbol)) {
      throw new IllegalArgumentException(
          "Position symbol mismatch: expected " + symbol + ", got " + newPosition.symbol());
    }

    if (newPosition.totalQuantity().isZero()) {
      // Position closed - remove it
      positions.remove(symbol);
    } else {
      // Position updated/created
      positions.put(symbol, newPosition);
    }

    touch();
  }

  public void clearPosition(AssetSymbol symbol) {
    positions.remove(symbol); // ONLY removes position state
    touch();
  }

  public Position ensurePosition(AssetSymbol symbol, AssetType assetType) {
    return positions.computeIfAbsent(symbol, s -> createEmptyPosition(s, assetType));
  }

  public void beginReplay() {
    if (this.state == LifecycleState.CLOSED) {
      throw new IllegalStateException("Cannot replay a closed account");
    }
    this.state = LifecycleState.REPLAYING;

    // The "Contract": Resetting state is now internal and guaranteed
    this.cashBalance = Money.zero(this.accountCurrency);
    this.positions.clear();
    this.realizedGains = new ArrayList<>();
  }

  public void endReplay() {
    if (this.state != LifecycleState.REPLAYING) {
      throw new IllegalStateException("Account is not in replay mode");
    }
    this.state = LifecycleState.ACTIVE;
    // Optional: Trigger a validation check here to ensure
    // the final state is sane before going live.
  }

  public boolean isInReplayMode() {
    return this.state == LifecycleState.REPLAYING;
  }

  // accounts should be closed via portfolio
  void close() {
    if (this.state == LifecycleState.REPLAYING) {
      throw new IllegalStateException("Cannot close account during replay");
    }

    requireActive();

    if (!positions.isEmpty()) {
      throw new IllegalStateException("Cannot close account with open positions");
    }
    if (cashBalance.isPositive()) {
      throw new IllegalStateException("Cannot close account with cash balance");
    }

    this.state = LifecycleState.CLOSED;
    this.closeDate = Instant.now();
    touch();
  }

  void reopen() {
    if (state != LifecycleState.CLOSED) {
      throw new IllegalStateException("Can only reopen a closed account. Current state: " + state);
    }
    this.state = LifecycleState.ACTIVE;
    this.closeDate = null;
    touch();
  }

  public void markStale() {
    this.healthStatus = HealthStatus.STALE;
  }

  public void restoreHealth() {
    this.healthStatus = HealthStatus.HEALTHY;
  }

  public List<RealizedGainRecord> getRealizedGains() {
    return Collections.unmodifiableList(realizedGains);
  }

  public boolean isStale() {
    return this.healthStatus == HealthStatus.STALE;
  }

  public Money getTotalRealizedGainLoss() {
    return realizedGains.stream().map(RealizedGainRecord::realizedGainLoss)
        .reduce(Money.zero(accountCurrency), Money::add);
  }

  public List<RealizedGainRecord> getRealizedGainsFor(AssetSymbol symbol) {
    notNull(symbol, "symbol");
    return realizedGains.stream().filter(r -> r.symbol().equals(symbol)).toList();
  }

  public Collection<Position> getAllPositions() {
    return positions.values().stream().map(Position::copy).toList();
  }

  public Collection<Map.Entry<AssetSymbol, Position>> getPositionEntries() {
    return positions.entrySet().stream()
        .map(entry -> Map.entry(entry.getKey(), entry.getValue().copy())).toList();
  }

  public int getPositionCount() {
    return positions.size();
  }

  public Optional<Position> getPosition(AssetSymbol symbol) {
    Position position = positions.get(symbol);
    return Optional.ofNullable(position != null ? position.copy() : null);
  }

  public boolean hasPosition(AssetSymbol symbol) {
    return positions.containsKey(symbol);
  }

  public boolean hasSufficientCash(Money requiredAmount) {
    validateCurrency(requiredAmount);
    return cashBalance.amount().compareTo(requiredAmount.amount()) >= 0;
  }

  public void updateName(String newName) {
    if (newName == null || newName.trim().isEmpty()) {
      throw new IllegalArgumentException("Account name cannot be empty");
    }
    this.name = newName.trim();
    touch();
  }

  public boolean isActive() {
    return this.state != LifecycleState.CLOSED;
  }

  private Position createEmptyPosition(AssetSymbol symbol, AssetType assetType) {
    return switch (positionStrategy) {
      case ACB -> AcbPosition.empty(symbol, assetType, accountCurrency);
      case FIFO -> FifoPosition.empty(symbol, assetType, accountCurrency);
      case LIFO -> throw new IllegalArgumentException("LIFO NOT SUPPORTED YET");
      case SPECIFIC_ID -> throw new IllegalArgumentException("SPECIFIC_ID NOT SUPPORTED YET");
    };
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

  public enum LifecycleState {
    ACTIVE,
    REPLAYING,
    CLOSED
  }
}
