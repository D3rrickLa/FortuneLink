package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

// no need for portoflio pref because think about it... a portfolio is just container
// so that means no calculation and recording of transaction, just manage a collectiosn of accounts
public class Portfolio implements ClassValidation {
    private final PortfolioId portfolioId;
    private final UserId userId;
    private String name;
    private String description;
    private Map<AccountId, Account> accounts;

    private boolean deleted;
    private Instant deletedOn;
    private UserId deletedBy;

    private final Instant createdAt;
    private Instant lastUpdatedOn;

    // private full args constructor
    private Portfolio(PortfolioId portfolioId, UserId userId, String name, String description,
            Map<AccountId, Account> accounts, boolean deleted, Instant deletedOn,
            UserId deletedBy, Instant createdAt, Instant lastUpdatedOn) {
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.accounts = new HashMap<>(accounts);
        this.deleted = deleted;
        this.deletedOn = deletedOn;
        this.deletedBy = deletedBy;
        this.createdAt = createdAt;
        this.lastUpdatedOn = lastUpdatedOn;
    }

    protected Portfolio() {
        this.portfolioId = null;
        this.userId = null;
        this.createdAt = null;
    }

    public Portfolio(PortfolioId portfolioId, UserId userId, String name) {
        ClassValidation.validateParameter(portfolioId, "portfolioId");
        ClassValidation.validateParameter(userId, "userId");

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be empty");
        }

        this.portfolioId = portfolioId;
        this.userId = userId;
        this.name = name.trim();
        this.description = "";
        this.accounts = new HashMap<>();
        this.deleted = false;
        this.deletedOn = null;
        this.deletedBy = null;
        this.createdAt = Instant.now();
        this.lastUpdatedOn = Instant.now();
    }

    public static Portfolio createNew(UserId userId, String name, String description) {
        Instant now = Instant.now();
        String cleanDesc = description == null ? "" : description;
        return new Portfolio(
                PortfolioId.newId(), // ← Generated ID
                userId,
                name,
                cleanDesc,
                new HashMap<>(), // ← Empty accounts
                false, // ← Not deleted
                null, // ← No deletion date
                null, // ← No deleter
                now, // ← Creation timestamp
                now // ← Last updated
        );
    }

    // Static factory for reconstitution
    public static Portfolio reconstitute(PortfolioId portfolioId, UserId userId, String name, String description,
            Map<AccountId, Account> accounts, boolean deleted, Instant deletedOn, UserId deletedBy, Instant createdAt,
            Instant lastUpdatedOn) {
        return new Portfolio(portfolioId, userId, name, description, accounts, deleted, deletedOn, deletedBy, createdAt,
                lastUpdatedOn);
    }

    public Account createAccount(String name, AccountType type, Currency currency, PositionStrategy strategy) {
        ClassValidation.validateParameter(name, "name");
        ClassValidation.validateParameter(type, "type");
        ClassValidation.validateParameter(currency, "currency");
        ClassValidation.validateParameter(strategy, "strategy");

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }

        if (accountNameExists(name)) {
            throw new IllegalArgumentException("Account name already exists: " + name);
        }

        AccountId accountId = AccountId.newId();
        Account account = new Account(accountId, name, type, currency, strategy);

        accounts.put(accountId, account);
        touch();

        return account;
    }

    public void renameAccount(AccountId accountId, String newName) {
        ClassValidation.validateParameter(accountId, "accountId");

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }

        Account account = getAccount(accountId);

        // Check if new name conflicts with another account
        // 1. If the name is exactly the same, just exit early (Success)
        if (account.getName().equalsIgnoreCase(newName)) {
            return;
        }

        // 2. Now check if someone ELSE is already using the name
        if (accountNameExists(newName)) {
            throw new IllegalArgumentException("Account name already exists: " + newName);
        }

        account.updateName(newName);
        touch();
    }

    public void closeAccount(AccountId accountId) {
        ClassValidation.validateParameter(accountId, "accountId");

        Account account = getAccount(accountId);
        account.close(); // Account validates internally
        touch();
    }

    public void reopenAccount(AccountId accountId) {
        ClassValidation.validateParameter(accountId, "accountId");

        Account account = getAccount(accountId);
        account.reopen();
        touch();
    }

    public void removeAccount(AccountId accountId) {
        ClassValidation.validateParameter(accountId, "accountId");

        Account account = getAccount(accountId);

        if (account.isActive()) {
            throw new IllegalStateException(
                    "Cannot remove active account. Close it first: " + account.getName());
        }

        accounts.remove(accountId);
        touch();
    }

    public void updateDetails(String newName, String newDescription) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be empty");
        }

        this.name = newName.trim();
        this.description = newDescription != null ? newDescription.trim() : "";
        touch();
    }

    public void markAsDeleted(UserId deletingUser) {
        ClassValidation.validateParameter(deletingUser, "deletingUser");

        if (deleted) {
            throw new IllegalStateException("Portfolio is already deleted");
        }

        if (!accounts.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete portfolio with " + accounts.size() + " account(s). " +
                            "Close and remove all accounts first.");
        }

        this.deleted = true;
        this.deletedOn = Instant.now();
        this.deletedBy = deletingUser;
        touch();
    }

    public void restore() {
        if (!deleted) {
            throw new IllegalStateException("Portfolio is not deleted");
        }

        this.deleted = false;
        this.deletedOn = null;
        this.deletedBy = null;
        touch();
    }

    public Account getAccount(AccountId accountId) {
        ClassValidation.validateParameter(accountId, "accountId");

        Account account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException(String.format("%s not found in %s", accountId, this.portfolioId));
        }
        return account;
    }

    public Optional<Account> findAccount(AccountId accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    public Optional<Account> findAccountByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }

        return accounts.values().stream()
                .filter(a -> a.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    public List<Account> findAccountsByType(AccountType type) {
        ClassValidation.validateParameter(type, "type");

        return accounts.values().stream()
                .filter(a -> a.getAccountType().equals(type))
                .toList();

    }

    public Collection<Account> getAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    public boolean hasAccounts() {
        return !accounts.isEmpty();
    }

    public int getAccountCount() {
        return accounts.size();
    }

    public boolean belongsToUser(UserId userId) {
        return this.userId.equals(userId);
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedOn() {
        return deletedOn;
    }

    public UserId getDeletedBy() {
        return deletedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedOn() {
        return lastUpdatedOn;
    }

    private boolean accountNameExists(String name) {
        return accounts.values().stream()
                .anyMatch(a -> a.getName().equalsIgnoreCase(name));
    }

    private void touch() {
        this.lastUpdatedOn = Instant.now();
    }
}