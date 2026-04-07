package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// so we are going to actually add back a 'currency preference'
// it is only meant to be used to display your aggregate data
// in one currency
public class Portfolio {
  private final PortfolioId portfolioId;
  private final UserId userId;
  private final Instant createdAt;
  private String name;
  private String description;
  private Map<AccountId, Account> accounts;
  private Currency displayCurrency;
  private boolean deleted;
  private Instant deletedOn;
  private UserId deletedBy;
  private Instant lastUpdatedAt;

  // private full args constructor
  private Portfolio(PortfolioId portfolioId, UserId userId, String name, String description,
      Map<AccountId, Account> accounts, Currency displayCurrency, boolean deleted,
      Instant deletedOn, UserId deletedBy, Instant createdAt, Instant lastUpdatedAt) {
    this.portfolioId = notNull(portfolioId, "portfolioId");
    this.userId = notNull(userId, "userId");
    this.name = notNull(name, "name");
    this.description = description;
    this.accounts = new HashMap<>(accounts);
    this.displayCurrency = notNull(displayCurrency, "displayCurrency");
    this.deleted = deleted;
    this.deletedOn = deletedOn;
    this.deletedBy = deletedBy;
    this.createdAt = createdAt;
    this.lastUpdatedAt = lastUpdatedAt;
  }

  protected Portfolio() {
    this.portfolioId = null;
    this.userId = null;
    this.createdAt = null;
  }

  public static Portfolio createNew(UserId userId, String name, String description,
      Currency displayCurrency) {
    Instant now = Instant.now();
    String cleanDesc = description == null ? "" : description;
    return new Portfolio(PortfolioId.newId(), // <- Generated ID
        userId, name, cleanDesc, new HashMap<>(), // <- Empty accounts
        displayCurrency, false, // <- Not deleted
        null, // <- No deletion date
        null, // <- No deleter
        now, // <- Creation timestamp
        now // <- Last updated
    );
  }

  // Static factory for reconstitution
  public static Portfolio reconstitute(PortfolioId portfolioId, UserId userId, String name,
      String description, Map<AccountId, Account> accounts, Currency displayCurrency,
      boolean deleted, Instant deletedOn, UserId deletedBy, Instant createdAt,
      Instant lastUpdatedOn) {
    return new Portfolio(portfolioId, userId, name, description, accounts, displayCurrency, deleted,
        deletedOn, deletedBy, createdAt, lastUpdatedOn);
  }

  public Account createAccount(String name, AccountType type, Currency currency,
      PositionStrategy strategy) {
    notNull(name, "name");
    notNull(type, "type");
    notNull(currency, "currency");
    notNull(strategy, "strategy");

    // Guard, supports ACB only for now
    if (strategy != PositionStrategy.ACB) {
      throw new IllegalArgumentException(
          "Only ACB strategy is supported. " + strategy + " is not available.");
    }

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

  // Check if new name conflicts with another account
  public void renameAccount(AccountId accountId, String newName) {
    notNull(accountId, "accountId");

    if (newName == null || newName.trim().isEmpty()) {
      throw new IllegalArgumentException("Account name cannot be empty");
    }

    Account account = getAccount(accountId);
    
    if (account.getState() == AccountLifecycleState.CLOSED) {
      throw new IllegalStateException("Cannot rename a closed account.");
    }

    if (account.getName().equalsIgnoreCase(newName)) {
      return;
    }

    if (accountNameExists(newName)) {
      throw new IllegalArgumentException("Account name already exists: " + newName);
    }

    account.updateName(newName);
    touch();
  }

  public void closeAccount(AccountId accountId) {
    notNull(accountId, "accountId");

    Account account = getAccount(accountId);
    account.close(); // Account validates internally
    touch();
  }

  public void reopenAccount(AccountId accountId) {
    notNull(accountId, "accountId");

    Account account = getAccount(accountId);
    account.reopen();
    touch();
  }

  public void removeAccount(AccountId accountId) {
    notNull(accountId, "accountId");

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
    notNull(deletingUser, "deletingUser");

    if (deleted) {
      throw new PortfolioAlreadyDeletedException("Portfolio is already deleted");
    }

    // Check only ACTIVE accounts, closed accounts don't block deletion
    boolean hasActiveAccounts = accounts.values().stream().anyMatch(Account::isActive);
    if (hasActiveAccounts) {
      long activeCount = accounts.values().stream().filter(Account::isActive).count();
      throw new PortfolioNotEmptyException(
          "Cannot delete portfolio with " + activeCount + " active account(s). "
              + "Close all accounts first.");
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

  public void reportRecalculationFailure(AccountId accountId) {
    Account account = findAccount(accountId).orElseThrow(
        () -> new AccountNotFoundException(accountId, portfolioId));
    account.markStale();
    touch();
  }

  public void reportRecalculationSuccess(AccountId accountId) {
    Account account = findAccount(accountId).orElseThrow(
        () -> new AccountNotFoundException(accountId, portfolioId));

    if (account.isStale()) {
      account.restoreHealth();
      touch();
    }
  }

  public void updateDisplayCurrency(Currency newCurrency) {
    this.displayCurrency = notNull(newCurrency, "displayCurrency");
    this.lastUpdatedAt = Instant.now();
  }

  public Account getAccount(AccountId accountId) {
    notNull(accountId, "accountId");

    Account account = accounts.get(accountId);
    if (account == null) {
      throw new AccountNotFoundException(accountId, this.portfolioId);
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

    return accounts.values().stream().filter(a -> a.getName().equalsIgnoreCase(name.trim()))
        .findFirst();
  }

  public List<Account> findAccountsByType(AccountType type) {
    notNull(type, "type");
    return accounts.values().stream().filter(a -> a.getAccountType().equals(type)).toList();
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

  // -- Getters ---
  public PortfolioId getPortfolioId() {
    return portfolioId;
  }

  public UserId getUserId() {
    return userId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Currency getDisplayCurrency() {
    return displayCurrency;
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

  public Instant getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  private boolean accountNameExists(String name) {
    return accounts.values().stream().anyMatch(a -> a.getName().equalsIgnoreCase(name));
  }

  private void touch() {
    this.lastUpdatedAt = Instant.now();
  }
}