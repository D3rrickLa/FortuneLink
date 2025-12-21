package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.time.Instant;
import java.time.InstantSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Portfolio implements ClassValidation {
    private final PortfolioId portfolioId;
    private final UserId userId;
    private List<Account> accounts;
    private ValidatedCurrency portfolioCurrency;

    private final Instant systemCreationDate;
    private Instant lastUpdatedAt;

    private Portfolio(PortfolioId portfolioId, UserId userId, List<Account> accounts, ValidatedCurrency portfolioCurrency, Instant systemCreationDate, Instant updatedAt) {
        this.portfolioId = ClassValidation.validateParameter(portfolioId);
        this.userId = ClassValidation.validateParameter(userId);
        this.accounts = ClassValidation.validateParameter(accounts);
        this.portfolioCurrency = ClassValidation.validateParameter(portfolioCurrency);
        this.systemCreationDate = ClassValidation.validateParameter(systemCreationDate);
        this.lastUpdatedAt = ClassValidation.validateParameter(updatedAt);
    }
    
    // Constructor for new portfolio
    public Portfolio(UserId userId, ValidatedCurrency currency) {
        this(PortfolioId.randomId(), Objects.requireNonNull(userId, "UserId cannot be null"), new ArrayList<>(), currency, Instant.now(), Instant.now());
    }

    // Constructor for reconstitution from repository
    public Portfolio(PortfolioId id, UserId userId, List<Account> accounts, List<Transaction> transactionHistory, Instant createdDate, Instant lastUpdated) {
        this(
            Objects.requireNonNull(id, "PortfolioId cannot be null"),
            Objects.requireNonNull(userId, "UserId cannot be null"),
            new ArrayList<>(accounts),
            ValidatedCurrency.USD, // default portfolio currency preference
            createdDate,
            lastUpdated
        );
    }
}

public class Portfolio2 implements ClassValidation {
    private final PortfolioId portfolioId;
    private final UserId userId;
    private List<Account> accounts; 
    private ValidatedCurrency portfolioCurrency; // master currency for doing all the evaluation

    // NOTE: this will be a method call
    // private List<Transaction> transactionHistory; // we might still have that problem of scaling, but will ignore for now
    private final Instant systemCreationDate;
    private Instant updatedAt;

    private Portfolio(PortfolioId portfolioId, UserId userId, List<Account> accounts, 
        ValidatedCurrency portfolioCurrency, Instant systemCreationDate, Instant updatedAt) {
        this.portfolioId = ClassValidation.validateParameter(portfolioId);
        this.userId = ClassValidation.validateParameter(userId);
        this.accounts = ClassValidation.validateParameter(accounts);
        this.portfolioCurrency = ClassValidation.validateParameter(portfolioCurrency);
        this.systemCreationDate = ClassValidation.validateParameter(systemCreationDate);
        this.updatedAt = ClassValidation.validateParameter(updatedAt);
    }

    // Constructor for reconstitution from repository
    // this will be a problem, i don't know where the transactions live without a function to 'place them' 
    // in the right account
    // public Portfolio(PortfolioId id, UserId userId, List<Account> accounts, 
    //                 List<Transaction> transactionHistory, LocalDateTime createdDate, 
    //                 LocalDateTime lastUpdated) {
    //     this.portfolioId = Objects.requireNonNull(id, "PortfolioId cannot be null");
    //     this.userId = Objects.requireNonNull(userId, "UserId cannot be null");
    //     this.accounts = new ArrayList<>(accounts);
    //     this.transactionHistory = new ArrayList<>(transactionHistory);
    //     this.createdDate = createdDate;
    //     this.lastUpdated = lastUpdated;
    // }

    public Portfolio(UserId userId, ValidatedCurrency portfolioCurrency) {
        this(PortfolioId.randomId(), userId, new ArrayList<>(), portfolioCurrency, Instant.now(), Instant.now());
    }

  
    public void addAccount(Account account) {
        Objects.requireNonNull(account);
        if (this.accounts.stream().anyMatch(a -> a.getAccountId().equals(account.getAccountId()))) {
            throw new IllegalStateException("Account with ID " + account.getAccountId() + " already exists");
        }

        if (this.accounts.stream().anyMatch(a -> a.getName().equalsIgnoreCase(account.getName()))) {
            throw new IllegalStateException("Account with name '" + account.getName() + "' already exists");
        }

        this.accounts.add(account);
        updateMetadata();    
    }

    public void removeAccount(AccountId accountId) throws AccountNotFoundException {
        Objects.requireNonNull(accountId);
        Account account = getAccount(accountId);

        if (!account.getAssets().isEmpty()) {
            throw new IllegalStateException(String.format("Cannot remove account '%s' - it still contains assets or cash", account.getName()));
        }

        // this makes no sense if you think about it... you should be able to, but what about history, what do we do with it?
        
        // boolean hasTransaction = transactionHistory.stream()
        //     .anyMatch(tx -> tx.getAccountId().equals(accountId));

        // if (hasTransaction) {
        //     throw new IllegalStateException(String.format("Cannot remove account '%s'  - it has transaction history", account.getName()));
        // }

        this.accounts.remove(account);
        updateMetadata();    
    }

    public void updateTransaction(AccountId accountId, TransactionId transactionId, Transaction updatedTransaction) {
        Account existingAccount = this.accounts.stream()
            .filter(a -> a.getAccountId().equals(accountId))
            .findFirst()
            .orElseThrow(() -> new AccountNotFoundException("Account not found when trying to update this transaction"));
    
        existingAccount.updateTransaction(transactionId, updatedTransaction);

    }

    public void removeTransaction(AccountId accountId, TransactionId transactionId) {
        Account existingAccount = this.accounts.stream()
            .filter(a -> a.getAccountId().equals(accountId))
            .findFirst()
            .orElseThrow(() -> new AccountNotFoundException("Account not found when trying to remove this transaction"));

        existingAccount.removeTransaction(transactionId);
    }

    public void recordTransaction(AccountId accountId, Transaction transaction) throws AccountNotFoundException {
        Account account = getAccount(accountId);

        account.recordTransaction(transaction);
        updateMetadata();
    }

    // GET LOGIC
    public Account getAccount(AccountId accountId) throws AccountNotFoundException {
        return this.accounts.stream()
            .filter(a -> a.getAccountId().equals(accountId))
            .findFirst()
            .orElseThrow(() ->new AccountNotFoundException("Account cannot be found in portfolio"));
    } 

    public Money getTotalAssets(MarketDataService marketDataService, ExchangeRateService exchangeRateService) {
        Objects.requireNonNull(marketDataService, "marketDataService required");
        Objects.requireNonNull(exchangeRateService, "exchangeRateService required");

        return accounts.stream()
            .map(account -> {
                Money amount = account.calculateTotalValue(marketDataService); // may be in different currency
                return exchangeRateService.convert(amount, portfolioCurrency); // convert to portfolioCurrency (e.g., CAD)
            }) 
            .reduce(Money.ZERO(this.portfolioCurrency), Money::add);
    }

    // chronological order
    public List<Transaction> getTransactionHistory(Instant startDate, Instant endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }

        return this.accounts.stream()
        // flatMap -> to collect all transactions from all accounts.
            .flatMap(account -> account.getTransactions().stream())
            .filter(tx -> {
                Instant txDate = tx.getTransactionDate();
                if (startDate == null) {
                    return !txDate.isAfter(endDate); // everything up to endDate
                }
                return !txDate.isBefore(startDate) && !txDate.isAfter(endDate); // between start and end
            })
            .sorted(Comparator.comparing(Transaction::getTransactionDate))  // for chrono order
            .toList();
    }

    public List<Transaction> getTransactionsForAccount(AccountId accountId) throws Exception {
        Account account = getAccount(accountId); // throws if not found
        return account.getTransactions();
    }

    public List<Transaction> getTransactionsForAsset(AssetIdentifier assetIdentifier) {
        return accounts.stream()
            .flatMap(account -> account.getTransactions().stream())
            .filter(tx -> assetIdentifier.equals(tx.getAssetIdentifier()))
            .toList();
    }

    // more general purpose query
    public List<Transaction> queryTransactions(AccountId accountId, AssetIdentifier assetIdentifier, Instant startDate, Instant endDate) {
    return accounts.stream()
        .filter(account -> accountId == null || account.getAccountId().equals(accountId))
        .flatMap(account -> account.getTransactions().stream())
        .filter(tx -> assetIdentifier == null || assetIdentifier.equals(tx.getAssetIdentifier()))
        .filter(tx -> {
            Instant txDate = tx.getTransactionDate();
            return (startDate == null || !txDate.isBefore(startDate)) &&
                   (endDate == null || !txDate.isAfter(endDate));
        })
        .toList();
    }
    
    public Money calculateNetWorth(MarketDataService marketDataService, ExchangeRateService exchangeRateService) {
        return getTotalAssets(marketDataService, exchangeRateService);
    }

    public boolean isEmpty() {
        return this.getAccounts().isEmpty();
    }

    public int getTransactionCount() {
        long count = this.accounts.stream()
            .flatMap(a -> a.getTransactions().stream())
            .count();

        return (int) count;
    }

    private void updateMetadata() {
        this.updatedAt = Instant.now();
    }

}
