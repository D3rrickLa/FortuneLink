package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
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
@Builder // TODO: impl
public class Portfolio implements ClassValidation {
    private final PortfolioId portfolioId;
    private final UserId userId;
    private List<Account> accounts;
    private String name;
    private ValidatedCurrency portfolioCurrencyPreference;
    private String description;

    private boolean deleted;
    private Instant deletedAt;
    private UserId deletedBy; // Track who deleted it

    private final Instant systemCreationDate;
    private Instant lastUpdatedAt;

    private Portfolio(PortfolioId portfolioId, UserId userId, List<Account> accounts, String name,
            ValidatedCurrency portfolioCurrencyPreference, String description, boolean deleted, Instant deletedAt,
            UserId deletedBy, Instant systemCreationDate, Instant updatedAt) {
        this.portfolioId = ClassValidation.validateParameter(portfolioId);
        this.userId = ClassValidation.validateParameter(userId);
        this.accounts = ClassValidation.validateParameter(accounts);
        this.name = ClassValidation.validateParameter(name);
        this.description = ClassValidation.validateParameter(description);
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
        this.portfolioCurrencyPreference = ClassValidation.validateParameter(portfolioCurrencyPreference);
        this.systemCreationDate = ClassValidation.validateParameter(systemCreationDate);
        this.lastUpdatedAt = ClassValidation.validateParameter(updatedAt);
    }

    // Constructor for new portfolio
    public Portfolio(UserId userId, String name, ValidatedCurrency currency) {
        this(PortfolioId.randomId(), ClassValidation.validateParameter(userId, "UserId cannot be null"),
                new ArrayList<>(), name, currency, "My Portfolio", false, null, null, Instant.now(), Instant.now());
    }

    // Static Factory for Reconstitution (used by Mappers/Repositories)
    public static Portfolio reconstitute(
            PortfolioId id,
            UserId userId,
            List<Account> accounts,
            String name,
            ValidatedCurrency currency,
            String desc,
            boolean deleted, // ADD
            Instant deletedAt, // ADD
            UserId deletedBy, // ADD (if tracking)
            Instant created,
            Instant updated) {
        return new Portfolio(id, userId, accounts, name, currency, desc, deleted, deletedAt, deletedBy, created,
                updated);
    }

    // Static Factory for NEW Portfolios (used by Application Services)
    public static Portfolio createNew(UserId userId, ValidatedCurrency currency, String name, String desc) {
        Instant time = Instant.now();
        String descCleaned = desc == null ? " " : desc;
        return new Portfolio(
                new PortfolioId(UUID.randomUUID()),
                userId,
                new ArrayList<>(),
                name,
                currency,
                descCleaned,
                false,
                null,
                null,
                time, time);
    }

    // used ONLY in the update Portfolio in application layer
    public Portfolio updatePortfolio(String name, String description, ValidatedCurrency newDefaultCurrency) {
        this.portfolioCurrencyPreference = newDefaultCurrency;
        this.name = name;
        this.description = description;
        updateMetadata();
        return this;
    }

    public void addAccount(Account account) {
        ClassValidation.validateParameter(account, "Account");

        if (this.accounts.stream().anyMatch(a -> a.getAccountId().equals(account.getAccountId()))) {
            throw new IllegalStateException(
                    String.format("Account with ID, %s, already exists here.", account.getAccountId()));
        }

        if (this.accounts.stream().anyMatch(a -> a.getName().equals(account.getName()))) { // not ignore cases because
                                                                                           // it should be exact... when
                                                                                           // we check. We should allow
                                                                                           // though dumb, account_1 and
                                                                                           // AccOunt_1
            throw new IllegalArgumentException(
                    String.format("Account with name '%s' already exists in this account.", account.getName()));
        }

        this.accounts.add(account);
        updateMetadata();
    }

    public void closeAccount(AccountId accountId) {
        ClassValidation.validateParameter(accountId, "AccountId");
        getAccountOrThrow(accountId).close();
        updateMetadata();
    }

    /**
     * We are blocking any accounts with transactions in it. we will only 'delete'
     * accounts when no data is in it
     * as we need historical data for reporting so when an account is 'deleted' hide
     * it instead with 'closeAccount' method
     * 
     * @param accountId
     */
    public void removeAccount(AccountId accountId) {
        ClassValidation.validateParameter(accountId, "AccountId");
        Account account = getAccountOrThrow(accountId);

        if (account.isActive()) {
            throw new IllegalStateException("Account cannot be removed, please close the account first");
        }

        if (!account.getTransactions().isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Account '%s' has transaction history and cannot be deleted from the database.",
                    account.getName()));
        }

        this.accounts.remove(account);
        updateMetadata();
    }

    public void recordTransaction(AccountId accountId, Transaction transaction) throws AccountNotFoundException {
        getAccountOrThrow(accountId).recordTransaction(transaction);
        updateMetadata();
    }

    public void updateTransaction(AccountId accountId, TransactionId transactionId, Transaction updatedTransaction) {
        Account existingAccount = this.accounts.stream()
                .filter(a -> a.getAccountId().equals(accountId))
                .findFirst()
                .orElseThrow(
                        () -> new AccountNotFoundException("Account not found when trying to update this transaction"));
        existingAccount.updateTransaction(transactionId, updatedTransaction);
    }

    public void removeTransaction(AccountId accountId, TransactionId transactionId) {
        Account existingAccount = this.accounts.stream()
                .filter(a -> a.getAccountId().equals(accountId))
                .findFirst()
                .orElseThrow(
                        () -> new AccountNotFoundException("Account not found when trying to remove this transaction"));

        existingAccount.removeTransaction(transactionId);
    }

    public void correctAssetTicker(AccountId accountId, AssetId wrongTickerAssetId, AssetIdentifier correctTicker) {
        Account account = getAccountOrThrow(accountId);
        Asset wrongAsset = account.getAsset(wrongTickerAssetId);

        if (wrongAsset.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            // Asset exists but has no quantity - just remove it
            account.removeAsset(wrongAsset.getAssetId());
            updateMetadata();
            return;
        }

        // Asset has quantity - need to swap via transactions
        Transaction sellWrong = new Transaction(
                TransactionId.randomId(),
                account.getAccountId(),
                TransactionType.SELL,
                wrongAsset.getAssetIdentifier(),
                wrongAsset.getQuantity(),
                wrongAsset.getCostPerUnit(),
                Money.ZERO(wrongAsset.getCurrency()),
                null,
                Instant.now(),
                "Correction: Wrong ticker entered",
                false);

        recordTransaction(accountId, sellWrong);

        Transaction buyCorrect = new Transaction(
                TransactionId.randomId(),
                account.getAccountId(),
                TransactionType.BUY,
                correctTicker,
                wrongAsset.getQuantity(),
                wrongAsset.getCostPerUnit(),
                Money.ZERO(wrongAsset.getCurrency()),
                null,
                Instant.now(),
                "Correction: Applied correct ticker",
                false);

        recordTransaction(accountId, buyCorrect);
    }

    // Update Portfolio Info STARTS //
    public void updateCurrencyPreference(ValidatedCurrency updatedCurrency) {
        ClassValidation.validateParameter(updatedCurrency);
        this.portfolioCurrencyPreference = updatedCurrency;
    }

    public void markAsDeleted(Instant deletedAt, UserId deletedBy) {
        if (this.deleted) {
            throw new PortfolioAlreadyDeletedException("Portfolio " + this.portfolioId + " is already deleted");
        }

        if (containsAccounts()) {
            throw new PortfolioNotEmptyException("Cannot delete portfolio with " + accounts.size() + " account(s)");
        }

        this.deleted = true;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy; // Audit: who performed the deletion
        updateMetadata();
    }

    // Update Portfolio Info ENDS //

    // Querying Methods STARTS //

    public Optional<Account> findAccount(AccountId accountId) {
        return this.accounts.stream()
                .filter(acc -> acc.getAccountId().equals(accountId))
                .findFirst();
    }

    public boolean containsAccounts() {
        return !this.getAccounts().isEmpty();
    }

    public boolean belongsToUser(UserId userId) {
        return this.userId.equals(userId);
    }

    /**
     * 
     * @param accountId
     * @param assetIdentifier
     * @param startDate
     * @param endDate
     * @return
     */
    public List<Transaction> queryTransactions(AccountId accountId, AssetIdentifier assetIdentifier, Instant startDate,
            Instant endDate) {
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
                .flatMap(account -> account.getTransactions().stream()) // flatMap -> to collect all transactions from
                                                                        // all accounts.
                .filter(tx -> {
                    Instant txDate = tx.getTransactionDate();
                    if (startDate == null) {
                        return !txDate.isAfter(endDate); // everything up to endDate
                    }
                    return !txDate.isBefore(startDate) && !txDate.isAfter(endDate); // between start and end
                })
                .sorted(Comparator.comparing(Transaction::getTransactionDate)) // for chrono order
                .toList();
    }

    public List<Transaction> getTransactionsFromAccount(AccountId accountId) throws AccountNotFoundException { // Formally
                                                                                                               // getTransactionsForAccount
        Optional<Account> account = findAccount(accountId);
        return account.get().getTransactions();
    }

    public List<Transaction> getTransactionsFromAsset(AssetIdentifier assetIdentifier) { // Formally
                                                                                         // getTranssactionForAccount
        return accounts.stream()
                .flatMap(account -> account.getTransactions().stream())
                .filter(tx -> assetIdentifier.equals(tx.getAssetIdentifier()))
                .toList();
    }

    public Money getAssetsTotalValue(MarketDataService marketDataService, ExchangeRateService exchangeRateService) { // Formally
                                                                                                                     // getTotalAssets
        ClassValidation.validateParameter(marketDataService, "marketDataService required");
        ClassValidation.validateParameter(exchangeRateService, "exchangeRateService required");

        return this.accounts.stream()
                .map(account -> {
                    Money amount = account.calculateTotalValue(marketDataService); // may be in different currency
                    return exchangeRateService.convert(amount, this.portfolioCurrencyPreference); // convert to
                                                                                                  // portfolioCurrency
                                                                                                  // (e.g., CAD)
                })
                .reduce(Money.ZERO(this.portfolioCurrencyPreference), Money::add);
    }

    // Querying Methods ENDS //

    // PRIVATE HELPER STARTS //

    private void updateMetadata() {
        this.lastUpdatedAt = Instant.now();
    }

    private Account getAccountOrThrow(AccountId accountId) { // internal portfolio logic
        return findAccount(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId, this.portfolioId));
    }
    // PRIVATE HELPER ENDS //
}
