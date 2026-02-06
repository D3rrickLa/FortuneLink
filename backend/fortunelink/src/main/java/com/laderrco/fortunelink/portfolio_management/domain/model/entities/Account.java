package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
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

    public void applyCashFlow(Money amount, String reason) {
        requireActive();
        ClassValidation.validateParameter(amount, "amount");

        // zeros allowed because think of rebates of 0. stupid but valid
        // we don't need to validate addition base on currency
        // because Money.java does the validation
        if (amount.isPositive()) {
            cashBalance = cashBalance.add(amount);
        } else if (amount.isNegative()) {
            Money absAmount = amount.negate();
            if (cashBalance.isLessThan(absAmount)) {
                throw new IllegalStateException("Insufficient cash balance for " + reason);
            }
            cashBalance = cashBalance.subtract(absAmount);
        }

        touch();
    }

    public void applyFee(Money feeAmount) {
        requireActive();
        ClassValidation.validateParameter(feeAmount, "feeAmount");

        if (!feeAmount.isPositive()) {
            throw new IllegalArgumentException("Fee must be positive");
        }

        if (cashBalance.isLessThan(feeAmount)) {
            throw new IllegalStateException("Insufficient cash balance to cover fee");
        }

        cashBalance = cashBalance.subtract(feeAmount);
        touch();
    }

    public void updatePosition(AssetSymbol symbol, Position newPosition) {
        requireActive();
        ClassValidation.validateParameter(symbol, "symbol");
        ClassValidation.validateParameter(newPosition, "newPosition");

        // Validate position belongs to this symbol
        if (!newPosition.symbol().equals(symbol)) {
            throw new IllegalArgumentException(
                    "Position symbol mismatch: expected " + symbol + ", got " + newPosition.symbol());
        }

        if (newPosition.getTotalQuantity().isZero()) {
            // Position closed - remove it
            positions.remove(symbol);
        } else {
            // Position updated/created
            positions.put(symbol, newPosition);
        }

        touch();
    }

    public boolean hasSufficientCash(Money requiredAmount) {
        validateCurrency(requiredAmount);
        return cashBalance.amount().compareTo(requiredAmount.amount()) >= 0;
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

    public Map<AssetSymbol, Position> getPositions() {
        return Collections.unmodifiableMap(positions);
    }

    public Optional<Position> getPosition(AssetSymbol symbol) {
        return Optional.ofNullable(positions.get(symbol));
    }

    public Position getOrCreateEmptyPosition(AssetSymbol symbol, AssetType assetType) {
        return positions.computeIfAbsent(
                symbol,
                s -> createEmptyPosition(s, assetType));
    }

    private Position createEmptyPosition(AssetSymbol symbol, AssetType assetType) {
        // This is where you'd use your position strategy if you had FIFO/LIFO
        // For now, assuming ACB
        return Position.emptyAcb(symbol, assetType, accountCurrency);
    }

    private void requireActive() {
        if (!isActive) {
            throw new IllegalStateException("Account is closed");
        }
    }

    private void validateCurrency(Money amount) {
        if (!amount.currency().equals(accountCurrency)) {
            throw new CurrencyMismatchException("Expected " + accountCurrency + " but got " + amount.currency());
        }
    }

    private void touch() {
        this.lastUpdatedOn = Instant.now();
    }

}
