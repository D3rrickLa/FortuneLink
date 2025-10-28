package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.security.auth.login.AccountNotFoundException;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InsufficientAssetsException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.TransactionDate;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfoliomanagement.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class Portfolio {
    private final PortfolioId portfolioId;
    private final UserId userId;
    private List<Account> accounts;
    private List<Transaction> transactionHistory;
    private LocalDateTime localDateTime;
    private LocalDateTime lastUpdated;

    
    private Portfolio(PortfolioId portfolioId, UserId userId, List<Account> accounts,
            List<Transaction> transactionHistory, LocalDateTime localDateTime, LocalDateTime lastUpdated) {
        Objects.requireNonNull(portfolioId);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(accounts);
        Objects.requireNonNull(transactionHistory);
        Objects.requireNonNull(localDateTime);
        Objects.requireNonNull(lastUpdated);
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.accounts = accounts;
        this.transactionHistory = transactionHistory;
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

        boolean hasTransaction = transactionHistory.stream()
            .anyMatch(tx -> tx.getAccountId().equals(accountId));

        if (hasTransaction) {
            throw new IllegalStateException(String.format("Cannot remove account '%s'  - it has transaction transaction history", account.getName()));
        }

        this.accounts.remove(account);
        updateMetadata();
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

    // transactoin recording // 
    public Transaction recordBuyTransaction(AccountId accountId, AssetIdentifier assetIdentifier, Quantity quantity, Price price, List<Fee> fees, TransactionDate transactionDate, String notes) throws AccountNotFoundException {

        Account account = getAccount(accountId);

        if (quantity.isZeroOrNegative()) {
            throw new IllegalArgumentException("Buy quantity must be positive");
        }

        if (price.isZeroOrNegative()) {
            throw new IllegalArgumentException("Buy price must be positive");
        }

        Transaction transaction = Transaction.createBuyTransaction(portfolioId, accountId, assetIdentifier, quantity, price, fees, transactionDate, notes);

        handleBuy(account, transaction);

        this.transactionHistory.add(transaction);
        updateMetadata();

        return transaction;
    }

    public Transaction recordSellTransaction(AccountId accountId, AssetIdentifier assetIdentifier, Quantity quantity, Price price, List<Fee> fees, TransactionDate transactionDate, String notes) throws AccountNotFoundException {
        
        Account account = getAccount(accountId);

        // business rule: Can't sell what you don't have
        Asset asset = account.getAsset(assetIdentifier); // this already throws 

        if (asset.getQuantity().isLessThan(quantity)) {
            throw new InsufficientAssetsException(String.format("Cannot sell %s of %s. Only %s is available", quantity, assetIdentifier, asset.getQuantity()));
        }

        Transaction transaction = Transaction.createSellTransaction(portfolioId, accountId, assetIdentifier, quantity, price, fees, transactionDate, notes);

        handleSell(account, transaction);
        this.transactionHistory.add(transaction);
        updateMetadata();
        return transaction;
    }

    public Transaction recordDepositTransaction(AccountId accountId, Price amount, List<Fee> fees, TransactionDate transactionDate, String notes) throws AccountNotFoundException {
        Account account = getAccount(accountId);

        if (amount.pricePerUnit().amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        Transaction transaction = Transaction.createDepositTransaction(portfolioId, accountId, amount, fees, transactionDate, notes);

        handleDeposit(account, transaction);
        this.transactionHistory.add(transaction);
        updateMetadata();

        return transaction;
    }

    public Transaction recordWithdrawalTransaction(AccountId accountId, Price amount, List<Fee> fees, TransactionDate transactionDate, String notes) throws AccountNotFoundException {
        
        Account account = getAccount(accountId);
        
        if (amount.pricePerUnit().amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        
        // Business rule: Can't withdraw more than available cash
        Money availableCash = account.getCashBalance(); // we don't have this
        if (availableCash.isLessThan(amount.pricePerUnit())) {
            throw new InsufficientFundsException("Cannot withdraw " + amount + ". Only " + availableCash + " available");
        }
        
        Transaction transaction = Transaction.createWithdrawalTransaction(portfolioId, accountId, amount, fees, transactionDate, notes);
        
        handleWithdrawal(account, transaction);
        this.transactionHistory.add(transaction);
        updateMetadata();
        
        return transaction;
    }

    public Transaction recordDividendTransaction(AccountId accountId, AssetIdentifier assetIdentifier, Money amount, TransactionDate transactionDate, String notes) throws AccountNotFoundException {
        Account account = getAccount(accountId);

        if (!account.hasAsset(assetIdentifier)) {
            throw new AssetNotFoundException(String.format("Cannot record dividend for %s - not owned in this account", assetIdentifier));
        }

        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Dividend amount must be positive");
        }

        Transaction transaction = Transaction.createDividendTransaction(portfolioId, accountId, assetIdentifier, Price.of(amount), transactionDate, notes);
        handleDividend(account, transaction);
        this.transactionHistory.add(transaction);
        updateMetadata();

        return transaction;
    }

    public Transaction recordInterestTransaction(AccountId accountId, AssetIdentifier assetIdentifier, Money amount, TransactionDate transactionDate, String notes) throws AccountNotFoundException {
        
        Account account = getAccount(accountId);
        
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Interest amount must be positive");
        }
        
        Transaction transaction = Transaction.createInterestTransaction(portfolioId, accountId, assetIdentifier, Price.of(amount), transactionDate, notes);
        
        handleInterest(account, transaction);
        this.transactionHistory.add(transaction);
        updateMetadata();
        
        return transaction;
    }

    public Money calculateNetWorth(MarketDataService marketDataService) {
        Objects.requireNonNull(marketDataService, "marketDataService required");
        return getTotalAssets(marketDataService);
    }

    public Money getTotalAssets(MarketDataService marketDataService) {
        Objects.requireNonNull(marketDataService, "marketDataService required");

        Currency baseCurrency;
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

    public List<Transaction> gettransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
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

    private void handleWithdrawal(Account account, Transaction transaction) {

    }

    private void handleDividend(Account account, Transaction transaction) {

    }

    private void handleInterest(Account account, Transaction transaction) {

    }

    private void updateMetadata() {
        this.lastUpdated = LocalDateTime.now();
    }
}
