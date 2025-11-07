package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.entities.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Getter;

@Getter
public class Account implements ClassValidation {
    /*
    * TODO: Account will hold both transaction and the asset, it makes sense if you think about it. a portoflio can have 1 account,
    * but that is the thing with the transaction, not the portfolio. Portfolio will be an aggregate to collect and display data
    */

    private final AccountId accountId;
    private String name;
    private AccountType accountType;
    private ValidatedCurrency baseCurrency; // the currency this account is opened in
    private Money cashBalance;
    private List<Asset> assets; // for NON =-cash assets only
    private List<Transaction> transactions;


    public Account(AccountId accountId, String name, AccountType accountType, ValidatedCurrency baseCurrency,
            Money cashBalance, List<Asset> asset, List<Transaction> transactions) {
        this.accountId = ClassValidation.validateParameter(accountId);
        this.name = ClassValidation.validateParameter(name);
        this.accountType = ClassValidation.validateParameter(accountType);
        this.baseCurrency = ClassValidation.validateParameter(baseCurrency);
        this.cashBalance = ClassValidation.validateParameter(cashBalance);
        this.assets = assets != null ? assets : Collections.emptyList();
        this.transactions = transactions != null ? transactions : Collections.emptyList();
    }
    
    void deposit(Money money) {
        // deposit amount currency must match 
    }

    void withdraw(Money money) {

    }

    void addAsset(Asset asset, Money pricePerUnit, BigDecimal quantity) { // validates cash available

    }

    // add proceeds to cash
    void removeAsset(AssetId assetId, Money pricePerUnit, BigDecimal quantity) {

    }

    void updateAsset(AssetId assetId, Asset updatedAsset) {

    }

    void addTransaction(Transaction transaction) {

    }

    void removeTransaction(TransactionId transactionId) {

    }

    void updateTransaction(TransactionId transactionId, Transaction updatedTransaction) {

    }

    public Asset getAsset(AssetIdentifier assetIdentifierId) {
        return null;
    }

    public Transaction getTransaction(Transaction transactionId) {
        return null;
    }

    public Money getCashBalance() {
        return null;
    }

    public Money calculateTotalValue(MarketDataService marketDataService) {
        return null;
    }



    private void recalculateStateAfterChange() {
        // recalc cash, assets, cost basis, etc.
    }

    
}
