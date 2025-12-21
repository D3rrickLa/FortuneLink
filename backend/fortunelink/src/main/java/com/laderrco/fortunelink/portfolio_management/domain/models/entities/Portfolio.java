package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
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
    private ValidatedCurrency portfolioCurrencyPreference;

    private final Instant systemCreationDate;
    private Instant lastUpdatedAt;

    private Portfolio(PortfolioId portfolioId, UserId userId, List<Account> accounts, ValidatedCurrency portfolioCurrencyPreference, Instant systemCreationDate, Instant updatedAt) {
        this.portfolioId = ClassValidation.validateParameter(portfolioId);
        this.userId = ClassValidation.validateParameter(userId);
        this.accounts = ClassValidation.validateParameter(accounts);
        this.portfolioCurrencyPreference = ClassValidation.validateParameter(portfolioCurrencyPreference);
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

    public void addAccount(Account account) {
        ClassValidation.validateParameter(account, "Account");

        if (this.accounts.stream().anyMatch(a -> a.getAccountId().equals(account.getAccountId()))) {
            throw new IllegalStateException(String.format("Account with ID, %s, already exists here.", account.getAccountId()));
        }

        if (this.accounts.stream().anyMatch(a -> a.getName().equals(account.getName()))) { // not ignore cases because it should be exact... when we check. We should allow though dumb, account_1 and AccOunt_1
            throw new IllegalArgumentException(String.format("Account with name '%s' already exists in this account.", account.getName()));
        }

        this.accounts.add(account);
        updateMetadata();
    }

    public void closeAccount(AccountId accountId) {
        ClassValidation.validateParameter(accountId, "AccountId");

        Account account = getAccount(accountId);
        account.close();
        updateMetadata();
    }

    /**
     * We are blocking any accounts with transactions in it. we will only 'delete' accounts when no data is in it
     * as we need historical data for reporting so when an account is 'deleted' hide it instead with 'closeAccount' method
     * @param accountId
     */
    public void removeAccount(AccountId accountId) {
        ClassValidation.validateParameter(accountId, "AccountId");

        // just a dedicated method
        Account account = getAccount(accountId);

        if (account.isActive()) {
            throw new IllegalStateException("Account cannot be removed, please close the account first");
        }

        if (!account.getAssets().isEmpty()) {
            throw new IllegalStateException(String.format("Cannot remove account'%s'. Account has existing assets, %d asset(s).", accountId.accountId(), account.getAssets().size()));
        }

        boolean hasTransaction  = account.getTransactions().isEmpty();

        if (hasTransaction) {
        throw new IllegalStateException(
            String.format("Cannot remove account '%s'. Account has transaction history. Closed accounts with history must be retained for audit purposes.",account.getName()));
        }

        this.accounts.remove(account);
        updateMetadata();
    }

    public void recordTransaction(AccountId accountId, Transaction transaction) throws AccountNotFoundException {
        Account account = getAccount(accountId);

        account.recordTransaction(transaction);
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

    public void removeAsset(AccountId accountId, AssetId assetId) {
        Account account = getAccount(accountId);
        account.removeAsset(assetId);
    }

    // Querying Methods STARTS //
    
    public Account getAccount(AccountId accountId) throws AccountNotFoundException {
        return this.accounts.stream()
            .filter(acc -> acc.getAccountId().equals(accountId))
            .findFirst()
            .orElseThrow(() -> new AccountNotFoundException(String.format("Account with id '%s' not found in this portfolio.", accountId.accountId())));
    }
    
    public boolean containsAccounts() { // named as isEmpty before
        return this.getAccounts().isEmpty();
    }
    
    /**
     * 
     * @param accountId
     * @param assetIdentifier
     * @param startDate
     * @param endDate
     * @return 
     */
    public List<Transaction> queryTransactions(AccountId accountId, AssetIdentifier assetIdentifier, Instant startDate, Instant endDate) {
        return this.accounts.stream()
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

    public long getTransactionCount() {
        long count = this.accounts.stream()
            .flatMap(a -> a.getTransactions().stream())
            .count();

        return count;
    }

    /**
     * 
     * @param startDate
     * @param endDate
     * @return a list of all transactions in chronological order
     */
    public List<Transaction> getTransactionHistory(Instant startDate, Instant endDate) {
        ClassValidation.validateParameter(endDate, "End date");

        return this.accounts.stream()
            .flatMap(account -> account.getTransactions().stream()) // flatMap -> to collect all transactions from all accounts.
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

    public List<Transaction> getTransactionsFromAccount(AccountId accountId) throws AccountNotFoundException { // Formally getTransactionsForAccount
        Account account = getAccount(accountId);
        return account.getTransactions();
    }

    public List<Transaction> getTransactionsFromAsset(AssetIdentifier assetIdentifier) { // Formally getTranssactionForAccount
        return accounts.stream()
            .flatMap(account -> account.getTransactions().stream())
            .filter(tx -> assetIdentifier.equals(tx.getAssetIdentifier()))
            .toList();
    }
    
    public Money getAssetsTotalValue(MarketDataService marketDataService, ExchangeRateService exchangeRateService) { // Formally getTotalAssets
        ClassValidation.validateParameter(marketDataService, "marketDataService required");
        ClassValidation.validateParameter(exchangeRateService, "exchangeRateService required");

        return this.accounts.stream()
            .map(account -> {
                Money amount = account.calculateTotalValue(marketDataService); // may be in different currency
                return exchangeRateService.convert(amount, this.portfolioCurrencyPreference); // convert to portfolioCurrency (e.g., CAD)
            }) 
            .reduce(Money.ZERO(this.portfolioCurrencyPreference), Money::add);
    }

    // Querying Methods ENDS //

    // PRIVATE HELPER STARTS //

    private void updateMetadata() {
        this.lastUpdatedAt = Instant.now();
    }

    // PRIVATE HELPER ENDS //
}

