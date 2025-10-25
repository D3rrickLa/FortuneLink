package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.security.auth.login.AccountNotFoundException;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfoliomanagement.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class Portfolio {
    private final PortfolioId portfolioId;
    private final UserId userId;
    private List<Account> accounts;
    private List<Transaction> transctionHistory;
    private LocalDateTime localDateTime;
    private LocalDateTime lastUpdated;

    

    private Portfolio(PortfolioId portfolioId, UserId userId, List<Account> accounts,
            List<Transaction> transctionHistory, LocalDateTime localDateTime, LocalDateTime lastUpdated) {
        Objects.requireNonNull(portfolioId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(accounts);
        Objects.requireNonNull(transctionHistory);
        Objects.requireNonNull(localDateTime);
        Objects.requireNonNull(lastUpdated);
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.accounts = accounts;
        this.transctionHistory = transctionHistory;
        this.localDateTime = localDateTime;
        this.lastUpdated = lastUpdated;
    }

    public static Portfolio create(UserId userId) {
        LocalDateTime now = LocalDateTime.now();
        return new Portfolio(
            PortfolioId.randomId(),
            userId,
            new ArrayList<>(), 
            new ArrayList<>(), 
            now,
            now
        );
    }

    public static Portfolio reconstitute(PortfolioId portfolioId, UserId userId, List<Account> accounts, List<Transaction> transactionHistory, LocalDateTime createdDate, LocalDateTime lastUpdated) {
            return new Portfolio(portfolioId, userId, accounts, transactionHistory, createdDate, lastUpdated);
        }

    // account management //
    public void addAccount(Account newAccount) {
        Objects.requireNonNull(newAccount, "account required");
        if (this.accounts.stream().anyMatch(a -> a.getAccountId().equals(newAccount.getAccountId()))) {
            throw new IllegalStateException("Account with ID " + newAccount.getAccountId() + " already exists");
        }

        if (this.accounts.stream().anyMatch(a -> a.getName().equalsIgnoreCase(newAccount.getName()))) {
            throw new IllegalStateException("Account with name '" + newAccount.getName() + "' already exists");
        }

        this.accounts.add(newAccount);
        updateMetadata();
    }

    public void removeAccount(AccountId accountId) throws AccountNotFoundException {
        Objects.requireNonNull(accountId, "accountId required");
        Account account = getAccount(accountId);

        if (!account.isEmpty()) {
            throw new IllegalStateException(String.format("Cannot remvoe account '%s' - it still contains assets or cash", account.getName()));
        }

        boolean hasTransaction = transctionHistory.stream()
            .anyMatch(tx -> tx.getAccountId().equals(accountId));

        if (hasTransaction) {
            throw new IllegalStateException(String.format("Cannot remove account '%s'  - it has transaction transaction history", account.getName()));
        }

        this.accounts.remove(account);
        updateMetadata();
    }

    public void recordTransaction(Transaction transaction) {

    }

    public Account getAccount(AccountId accountId) throws AccountNotFoundException {
        return this.accounts.stream()
            .filter(x -> x.getAccountId().equals(accountId))
            .findFirst()
            .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    public Optional<Account> findAccountContaining(AssetIdentifier assetIdentifier) {
        Objects.requireNonNull(assetIdentifier, "assetIdentifier required");
        return this.accounts.stream()
            .filter(account -> account.hasAsset(assetIdentifier))
            .findFirst();
            
    }

    public Money calculateNetWorth(MarketDataService marketDataService) {
        return null;
    }

    public Money getTotalAssets(MarketDataService marketDataService) {
        return null;
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
    }

    public UserId getUserId() {
        return userId;
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public List<Transaction> getTransctionHistory() {
        return transctionHistory;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public boolean isEmpty() {
        return accounts.isEmpty() || accounts.stream().allMatch(Account::isEmpty);
    }

    private void handleBuy(Account account, Transaction transaction) {

    }

    private void handleSell(Account account, Transaction transaction) {

    }

    private void handleDeposit(Account account, Transaction transaction) {

    }

    private void handleDividend(Account account, Transaction transaction) {

    }

    private void updateMetadata() {
        this.lastUpdated = LocalDateTime.now();
    }
}
