package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.entities.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
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
    private List<Asset> assets; // for NON =cash assets only
    private List<Transaction> transactions;

    private final Instant systemCreationDate;
    private Instant lastSystemInteraction; // for calculating when you last interacted with this asset
    private int version;


    public Account(AccountId accountId, String name, AccountType accountType, ValidatedCurrency baseCurrency,
            Money cashBalance, List<Asset> asset, List<Transaction> transactions) {
        this.accountId = ClassValidation.validateParameter(accountId);
        this.name = ClassValidation.validateParameter(name);
        this.accountType = ClassValidation.validateParameter(accountType);
        this.baseCurrency = ClassValidation.validateParameter(baseCurrency);
        this.cashBalance = ClassValidation.validateParameter(cashBalance);
        this.assets = assets != null ? assets : Collections.emptyList();
        this.transactions = transactions != null ? transactions : Collections.emptyList();
        this.systemCreationDate = Instant.now();
        this.lastSystemInteraction = this.systemCreationDate;
        this.version = 1;
    }
    
    void deposit(Money money) {
        // deposit amount currency must match 
        Objects.requireNonNull(money);
        if (!money.currency().equals(this.baseCurrency)) {
            throw new IllegalArgumentException("Cannot deposit money with different currency.");
        }
        if (money.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount deposited must be greater than 0.");
        }

        this.cashBalance.add(money);
        updateMetadata();
    }

    void withdraw(Money money) {
        Objects.requireNonNull(money);
        if (!money.currency().equals(this.baseCurrency)) {
            throw new IllegalArgumentException("Cannot withdraw money with different currency.");
        }
        if (money.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount withdrawn must be greater than 0.");
        }

        this.cashBalance.subtract(money);
        updateMetadata();
    }

    void addAsset(Asset asset) { // validates cash available
        Objects.requireNonNull(asset);
        boolean alreadyExists = this.assets.stream()
            .anyMatch(a -> a.getAssetIdentifier().getPrimaryId().equals(a.getAssetIdentifier().getPrimaryId()));

        if (alreadyExists) {
            throw new IllegalStateException(String.format("Asset with identifier %s already exists in this account", asset.getAssetIdentifier().getPrimaryId()));
        }
        this.assets.add(asset);
        updateMetadata();

    }

    // add proceeds to cash
    void removeAsset(AssetId assetId) {
        Objects.requireNonNull(assetId);
        boolean removed = this.assets.removeIf(a -> a.getAssetId().equals(assetId));
        if (removed) {
            updateMetadata();
        }
    }

    void updateAsset(AssetId assetId, Asset updatedAsset) {
        Objects.requireNonNull(assetId);
        Objects.requireNonNull(updatedAsset);
        // Verify the updatedAsset has the same ID
        if (!updatedAsset.getAssetId().equals(assetId)) {
            throw new IllegalArgumentException("Cannot change asset identity");
        }

        Optional<Asset> existingAsset = this.assets.stream()
            .filter(a -> a.getAssetId().equals(assetId))
            .findFirst();

        if (existingAsset.isEmpty()) {
            throw new IllegalStateException(assetId.toString());
        }
        
        // Replace the entire asset object
        this.assets.remove(existingAsset.get());
        this.assets.add(updatedAsset);
        updateMetadata();
    }

    void addTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction);
        boolean alreadyExists = this.transactions.stream()
            .anyMatch(t -> t.getTransacationId().equals(transaction.getTransacationId()));

        if (alreadyExists) {
            throw new IllegalStateException(String.format("Transaction with id %s already exists in this account", transaction.getTransacationId()));
        }
        this.transactions.add(transaction);
        updateMetadata();
    }

    void removeTransaction(TransactionId transactionId) {
        Objects.requireNonNull(transactionId);
        boolean removed = this.transactions.removeIf(t -> t.getTransacationId().equals(transactionId));
        if (removed) {
            updateMetadata();
        }
    }

    void updateTransaction(TransactionId transactionId, Transaction updatedTransaction) {
        Objects.requireNonNull(transactionId);
        Objects.requireNonNull(updatedTransaction);

        if (!updatedTransaction.getTransacationId().equals(transactionId)) {
            throw new IllegalArgumentException("Cannot change transaction identity");
        }

        Optional<Transaction> existingTransaction = this.transactions.stream()
            .filter(t -> t.getTransacationId().equals(transactionId))
            .findAny();

        if (existingTransaction.isEmpty()) {
            throw new IllegalStateException(transactionId.toString());
        }

        this.transactions.remove(existingTransaction.get());
        this.transactions.add(updatedTransaction);
        updateMetadata();
    }

    public Asset getAsset(AssetIdentifier assetIdentifierId) {
        Objects.requireNonNull(assetIdentifierId);
        return this.assets.stream()
            .filter(a -> a.getAssetIdentifier().equals(assetIdentifierId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException());
    }

    public Transaction getTransaction(TransactionId transactionId) {
        Objects.requireNonNull(transactionId);
        return this.transactions.stream()
            .filter(t -> t.getTransacationId().equals(transactionId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException());
    }


    public Money calculateTotalValue(MarketDataService marketDataService) {
        return null;
    }

   private void updateMetadata() {
        version++;
        this.lastSystemInteraction = Instant.now();
    }

    private void recalculateStateAfterChange() {
        // recalc cash, assets, cost basis, etc.
        cashBalance = Money.ZERO(baseCurrency);
        assets.clear();
        
        // Replay all transactions in order
        transactions.stream()
            .sorted(Comparator.comparing(Transaction::getDate))
            .forEach(this::applyTransaction);
        
    }

    
}
