package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
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

        // this makes no sense if you thinkg about it... you should be able to, but what about history, what do we do with it?
        
        // boolean hasTransaction = transactionHistory.stream()
        //     .anyMatch(tx -> tx.getAccountId().equals(accountId));

        // if (hasTransaction) {
        //     throw new IllegalStateException(String.format("Cannot remove account '%s'  - it has transaction history", account.getName()));
        // }

        this.accounts.remove(account);
        updateMetadata();    
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

    private void updateMetadata() {
        this.updatedAt = Instant.now();
    }

}
