package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
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

    // JPA constructor
    protected Portfolio() {
        this.portfolioId = null;
        this.userId = null;
        this.createdAt = null;
    }

    // Business constructor
    public Portfolio(PortfolioId portfolioId, UserId userId) {
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.name = "Default Portfolio";
        this.description = ""; 
        this.accounts = new HashMap<>();
        this.deleted = false; // todo in the getter methods for deletedOn and deleteBy, we should have an if check
        this.createdAt = Instant.now();
        this.lastUpdatedOn = Instant.now();
    }

    public Account createAccount(String name, AccountType type, Currency currency) {
        // Enforce uniqueness
        if (accountNameExists(name)) {
            throw new IllegalArgumentException("Account name already exists: " + name);
        }

        AccountId accountId = AccountId.newId();
        Account account = new Account(accountId, name, type, currency);

        accounts.put(accountId, account);
        touch();

        return account;
    }

    public void addAccount(Account account) {
        ClassValidation.validateParameter(account, "account");
        if (accounts.containsKey(account.getAccountId())) {
            throw new IllegalStateException("Account already exists in portfolio");
        }
    }

    public void closeAccount(AccountId accountId) {
        Account account = getAccount(accountId);
        account.close(); // Validates internally
        touch();
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

    public Collection<Account> getAccounts() {
        return Collections.unmodifiableCollection(accounts.values());
    }

    public Account getAccount(AccountId accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return account;
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
