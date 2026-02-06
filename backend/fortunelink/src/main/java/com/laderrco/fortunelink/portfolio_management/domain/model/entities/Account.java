package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

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

    void addCash(Money amount) {
        requireActive();
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Cash amount must be positive");
        }
        cashBalance = cashBalance.add(amount);
        touch();
    }

    void subtractCash(Money amount) {
        requireActive();

        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Cash amount must be positive");
        }

        if (cashBalance.isLessThan(amount)) {
            throw new IllegalStateException("Insufficient cash balance");
        }

        cashBalance = cashBalance.subtract(amount);
        touch();
    }

    void applyPosition(Position updatedPosition) {
        requireActive();
        ClassValidation.validateParameter(updatedPosition);

        if (!updatedPosition.accountCurrency().equals(accountCurrency)) {
            throw new CurrencyMismatchException("Currency mismatch when applying position");
        }

        if (updatedPosition.getTotalQuantity().isPositive()) {
            positions.put(updatedPosition.symbol(), updatedPosition);
        } else {
            positions.remove(updatedPosition.symbol());
        }

        touch();
    }

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

    public AccountId getAccountId() { return accountId; }
    public String getName() { return name; }
    public AccountType getAccountType() { return accountType; }
    public Currency getAccountCurrency() { return accountCurrency; }
    public Money getCashBalance() { return cashBalance; }
    public Instant getCreationDate() { return creationDate; }
    public boolean isActive() { return isActive; }
    public Instant getCloseDate() { return closeDate; }
    public Instant getLastUpdatedOn() { return lastUpdatedOn; }
    
    public Map<AssetSymbol, Position> getPositions() {
        return Collections.unmodifiableMap(positions);
    }
    
    public Optional<Position> getPosition(AssetSymbol symbol) {
        return Optional.ofNullable(positions.get(symbol));
    }

    private void requireActive() {
        if (!isActive) {
            throw new IllegalStateException("Account is closed");
        }
    }

    private void touch() {
        this.lastUpdatedOn = Instant.now();
    }

}
