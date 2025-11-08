package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Getter;

@Getter
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
        this.portfolioCurrency = ClassValidation.validateParameter(portfolioCurrency)
        this.systemCreationDate = ClassValidation.validateParameter(systemCreationDate);
        this.updatedAt = ClassValidation.validateParameter(updatedAt);
    }

    public Portfolio(UserId userId, ValidatedCurrency portfolioCurrency) {
        this(PortfolioId.randomId(), userId, new ArrayList<>(), portfolioCurrency, Instant.now(), Instant.now());
    }

  
    public void addAccount() {

    }

    public void removeAccount(AccountId accountId) {

    }

    public void recordTransaction(AccountId accountId, Transaction transaction) {

    }

    // GET LOGIC
    public Account getAccount(AccountId accountId) {
        return this.accounts.stream()
            .filter(a -> a.getAccountId().equals(accountId))
            .findFirst()
            .orElseThrow();
    } 

    public Money getTotalAssets(MarketDataService marketDataService) {
        Objects.requireNonNull(marketDataService, "marketDataService required");

        return accounts.stream()
            .map(account -> account.calculateTotalValue(marketDataService)) // TODO: based on the value we need to change the amount to baseCurrency
            .reduce(Money.ZERO(this.portfolioCurrency), Money::add);
    }

    public List<Transaction> getTransactionHistory(Instant startDate, Instant endDate) {
        // TODO: logic check so if both are empty, we assume all, and if start date empty, eveyrthing 
        // up until end date. end date can't be empty
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
            .toList();
    }
    
    public Money calculateNetWorth(MarketDataService marketDataService) {
        return getTotalAssets(marketDataService);
    }



}
