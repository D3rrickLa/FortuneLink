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
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.PositionResult;
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

    public void applyCashFlow(Money amount, String reason) {
        requireActive();
        ClassValidation.validateParameter(amount, "amount");

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

    public PositionResult recordTrade(Transaction tx) {
        requireActive();
        ClassValidation.validateParameter(tx, "transaction");

        AssetSymbol symbol = tx.execution().asset();
        Position current = positions.getOrDefault(
                symbol,
                AcbPosition.empty(symbol, tx.metadata().assetType(), accountCurrency) // polymorphic empty position
        );

        PositionResult result = current.apply(tx);

        if (!result.isNoChange()) {
            Position updated = result.getUpdatedPosition();
            if (updated.getTotalQuantity().isZero()) {
                positions.remove(symbol);
            } else {
                positions.put(symbol, updated);
            }
        }

        // Update cash balance if transaction has a cash delta
        if (tx.cashDelta() != null && !tx.cashDelta().isZero()) {
            Money newCash = cashBalance.add(tx.cashDelta());
            if (newCash.isNegative()) {
                throw new IllegalStateException("Insufficient cash balance after applying transaction");
            }
            cashBalance = newCash;
        }

        touch();
        return result;
    }

    /**
     * Transfers (in or out) are treated the same as trades, unless
     * domain rules require different handling.
     */
    public PositionResult recordTransfer(Transaction tx) {
        return recordTrade(tx);
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

    private void requireActive() {
        if (!isActive) {
            throw new IllegalStateException("Account is closed");
        }
    }

    private void touch() {
        this.lastUpdatedOn = Instant.now();
    }

}
