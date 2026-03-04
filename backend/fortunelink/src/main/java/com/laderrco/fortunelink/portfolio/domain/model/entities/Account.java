package com.laderrco.fortunelink.portfolio.domain.model.entities;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
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
import java.util.*;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

// presents and maintains current state
public class Account {
    private final AccountId accountId;
    private AccountType accountType;
    private final Currency accountCurrency;
    private String name;
    private final PositionStrategy positionStrategy; // ← Explicit
    private HealthStatus healthStatus;

    private Money cashBalance;
    private Map<AssetSymbol, Position> positions;
    private List<RealizedGainRecord> realizedGains;

    private final Instant creationDate;
    private boolean isActive;
    private Instant closeDate;
    private Instant lastUpdatedOn;

    /**
     * JPA-only constructor. Object is in a partially invalid state until
     * Hibernate fully hydrates it — do NOT call business methods on a proxy
     * before that happens.
     *
     * positions and cashBalance are initialized to safe empty defaults so that
     * defensive calls like getPositionEntries() and getCashBalance() on a
     * partially-hydrated proxy don't NPE. All other fields remain null and are
     * set by Hibernate via field injection.
     *
     * accountCurrency is null here, which means any method that calls
     * validateCurrency() will NPE until hydration completes. This is
     * intentional — it surfaces the problem loudly rather than silently
     * operating on wrong currency. Never pass a JPA proxy into business logic
     * before the owning transaction has loaded the full entity.
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
    }

    public Account(AccountId accountId, String name, AccountType accountType, Currency accountCurrency,
            PositionStrategy positionStrategy) {
        notNull(accountId, "accountId");
        notNull(name, "name");
        notNull(accountType, "accountType");
        notNull(accountCurrency, "accountCurrency");
        notNull(positionStrategy, "positionStrategy");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }

        this.accountId = accountId;
        this.name = name.trim();
        this.accountType = accountType;
        this.accountCurrency = accountCurrency;
        this.positionStrategy = positionStrategy;
        this.healthStatus = HealthStatus.HEALTHY;
        this.cashBalance = Money.ZERO(accountCurrency);
        this.positions = new HashMap<>();
        this.realizedGains = new ArrayList<>();
        this.creationDate = Instant.now();
        this.isActive = true;
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

        if (!amount.isPositive()) {
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
     *
     * Called by the replay service immediately after position.sell() so that
     * capital gains history is preserved on the account without requiring a
     * full transaction log replay to reconstruct it.
     *
     * This is the only place RealizedGainRecord entries are created.
     * Do NOT call this for unrealized gains — only on actual sell events.
     */
    public void recordRealizedGain(AssetSymbol symbol, Money realizedGainLoss, Money costBasisSold,
            Instant occurredAt) {
        requireActive();
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
        positions.remove(symbol);
        realizedGains.removeIf(record -> record.symbol().equals(symbol));
        touch();
    }

    // replaces the getOrCreateEmptyPosition method
    public Position ensurePosition(AssetSymbol symbol, AssetType assetType) {
        return positions.computeIfAbsent(symbol, s -> createEmptyPosition(s, assetType));
    }

    // accounts should be closed via portfolio
    void close() {
        requireActive();

        if (!positions.isEmpty()) {
            throw new IllegalStateException("Cannot close account with open positions");
        }
        if (cashBalance.isPositive()) {
            throw new IllegalStateException("Cannot close account with cash balance");
        }

        this.isActive = false;
        this.closeDate = Instant.now();
        touch();
    }

    void reopen() {
        if (isActive) {
            throw new IllegalStateException("Account is already active");
        }

        this.isActive = true;
        this.closeDate = null;
        touch();
    }

    public void markStale() {
        this.healthStatus = HealthStatus.STALE;
    }

    public void restoreHealth() {
        this.healthStatus = HealthStatus.HEALTHY;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public Currency getAccountCurrency() {
        return accountCurrency;
    }

    public PositionStrategy getPositionStrategy() {
        return positionStrategy;
    }

    public List<RealizedGainRecord> getRealizedGains() {
        return Collections.unmodifiableList(realizedGains);
    }

    public Money getCashBalance() {
        return cashBalance;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCloseDate() {
        return closeDate;
    }

    public Instant getLastUpdatedOn() {
        return lastUpdatedOn;
    }

    public boolean isStale() {
        return this.healthStatus == HealthStatus.STALE;
    }

    public Money getTotalRealizedGainLoss() {
        return realizedGains.stream()
                .map(RealizedGainRecord::realizedGainLoss)
                .reduce(Money.ZERO(accountCurrency), Money::add);
    }

    public List<RealizedGainRecord> getRealizedGainsFor(AssetSymbol symbol) {
        notNull(symbol, "symbol");
        return realizedGains.stream()
                .filter(r -> r.symbol().equals(symbol))
                .toList();
    }

    public Collection<Position> getAllPositions() {
        return positions.values().stream()
                .map(Position::copy)
                .toList();
    }

    public Collection<Map.Entry<AssetSymbol, Position>> getPositionEntries() {
        return positions.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().copy()))
                .toList();
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

    public void clearAllPositions() {
        this.positions = new HashMap<>();
        touch();
    }

    public void resetCashToZero() {
        this.cashBalance = Money.ZERO(this.accountCurrency);
        touch();
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
        if (!isActive) {
            throw new AccountClosedException("Account " + accountId + " is closed");
        }
    }

    private void validateCurrency(Money amount) {
        if (!amount.currency().equals(accountCurrency)) {
            throw new CurrencyMismatchException(
                    "Expected " + accountCurrency + " but got " + amount.currency());
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
