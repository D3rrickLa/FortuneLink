package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.entities.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Getter;

@Getter
public class Portfolio implements ClassValidation {
    private final PortfolioId portfolioId;
    private final UserId userId;
    private List<Account> accounts; 

    // NOTE: this will be a method call
    // private List<Transaction> transactionHistory; // we might still have that problem of scaling, but will ignore for now
    private final Instant systemCreationDate;
    private Instant updatedAt;
    private Portfolio(PortfolioId portfolioId, UserId userId, List<Account> accounts, Instant systemCreationDate, Instant updatedAt) {
        this.portfolioId = ClassValidation.validateParameter(portfolioId);
        this.userId = ClassValidation.validateParameter(userId);
        this.accounts = ClassValidation.validateParameter(accounts);
        this.systemCreationDate = ClassValidation.validateParameter(systemCreationDate);
        this.updatedAt = ClassValidation.validateParameter(updatedAt);
    }

    public Portfolio(UserId userId) {
        this(PortfolioId.randomId(), userId, new ArrayList<>(), Instant.now(), Instant.now());
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

        Currency baseCurrency = determineBaseCurrency();

        return accounts.stream()
            .map(account -> account.calculateTotalValue(marketDataService, baseCurrency))
            .reduce(Money.ZERO(baseCurrency), Money::add);
    }

    public List<Transaction> getTransactionHistory() {
        return null;
    }
    
    public Money calculateNetWorth(MarketDataService marketDataService) {
        return getTotalAssets(marketDataService);
    }
}
