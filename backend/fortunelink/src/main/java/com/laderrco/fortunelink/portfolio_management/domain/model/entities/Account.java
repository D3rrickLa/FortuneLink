package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountClosedException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

// presents and maintains current state
public class Account implements ClassValidation {
    private final AccountId accountId;
    private AccountType accountType;
    private String name;
    private Currency accountCurrency;

    private Money cashBalance;
    private Map<AssetSymbol, Position> positions;

    private final Instant creationDate;
    private boolean isActive;
    private Instant closeDate;
    private Instant lastUpdatedOn;

    protected Account() {
        this.accountId = null;
        this.creationDate = null;
    }

    public Account(AccountId accountId, String name, AccountType accountType, Currency accountCurrency) {
        ClassValidation.validateParameter(accountId, "accountId");
        ClassValidation.validateParameter(name, "name");
        ClassValidation.validateParameter(accountType, "accountType");
        ClassValidation.validateParameter(accountCurrency, "accountCurrency");

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }

        this.accountId = accountId;
        this.name = name.trim();
        this.accountType = accountType;
        this.accountCurrency = accountCurrency;
        this.cashBalance = Money.ZERO(accountCurrency);
        this.positions = new HashMap<>();
        this.creationDate = Instant.now();
        this.isActive = true;
        this.closeDate = null;
        this.lastUpdatedOn = Instant.now();
    }

    // DEPOSIT, WITHDRAWL, DIVIDEND, INTEREST
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
        requireActive();
        validateCurrency(amount);
        validateReason(reason);

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        if (cashBalance.isLessThan(amount)) {
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

    // dividend reinvestment + buy + sell
    public void updatePosition(AssetSymbol symbol, Position newPosition) {
        requireActive();
        ClassValidation.validateParameter(symbol, "symbol");
        ClassValidation.validateParameter(newPosition, "newPosition");

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

    Position ensurePosition(AssetSymbol symbol, AssetType assetType) {
        return positions.computeIfAbsent(symbol, s -> createEmptyPosition(s, assetType));
    }

    public boolean hasSufficientCash(Money requiredAmount) {
        validateCurrency(requiredAmount);
        return cashBalance.amount().compareTo(requiredAmount.amount()) >= 0;
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

    public Collection<Position> getAllPositions() {
        return positions.values().stream()
                .map(Position::copy)
                .collect(Collectors.toUnmodifiableList());
    }

    public int getPositionCount() {
        return positions.size();
    }

    public boolean hasPosition(AssetSymbol symbol) {
        return positions.containsKey(symbol);
    }

    public Position getOrCreateEmptyPosition(AssetSymbol symbol, AssetType assetType) {
        return positions.computeIfAbsent(
                symbol,
                s -> createEmptyPosition(s, assetType));
    }

    private Position createEmptyPosition(AssetSymbol symbol, AssetType assetType) {
        // This is where you'd use your position strategy if you had FIFO/LIFO
        // For now, assuming ACB
        return AcbPosition.empty(symbol, assetType, accountCurrency);
    }

    public void validate() {
        if (accountId == null) {
            throw new IllegalStateException("Account ID cannot be null");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Account name cannot be empty");
        }
        if (accountType == null) {
            throw new IllegalStateException("Account type cannot be null");
        }
        if (accountCurrency == null) {
            throw new IllegalStateException("Account currency cannot be null");
        }
        if (cashBalance == null) {
            throw new IllegalStateException("Cash balance cannot be null");
        }
        if (cashBalance.isNegative()) {
            throw new IllegalStateException("Cash balance cannot be negative: " + cashBalance);
        }
        if (positions == null) {
            throw new IllegalStateException("Positions cannot be null");
        }
        if (!isActive && closeDate == null) {
            throw new IllegalStateException("Inactive account must have close date");
        }
        if (isActive && closeDate != null) {
            throw new IllegalStateException("Active account cannot have close date");
        }
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
