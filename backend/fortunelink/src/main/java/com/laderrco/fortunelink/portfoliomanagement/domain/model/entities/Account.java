package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.CashAssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.shared.enums.Currency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class Account {
    private final AccountId accountId;
    private String name;
    private AccountType accountType;
    private Currency baseCurrency;
    private List<Asset> assets; // we use Ids outside your aggregate

    private final Instant systemCreationDate;
    private Instant lastSystemInteraction; // for calculating when you last interacted with this asset
    private int version;

    private Account(AccountId accountId, String name, AccountType accountType, Currency baseCurrency, List<Asset> assets, Instant systemCreationDate, Instant lastSystemInteraction, int version) {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(accountType);
        Objects.requireNonNull(baseCurrency);
        Objects.requireNonNull(assets);
        Objects.requireNonNull(systemCreationDate);

        this.accountId = accountId;
        this.name = name;
        this.accountType = accountType;
        this.baseCurrency = baseCurrency;
        this.assets = assets;
        this.systemCreationDate = systemCreationDate;
        this.lastSystemInteraction = lastSystemInteraction;
        this.version = version;
    }

    public Account(AccountId accountId, String name, AccountType accountType, Currency baseCurrency) {
        this(
            accountId, 
            name, 
            accountType, 
            baseCurrency, 
            new ArrayList<Asset>(), 
            Instant.now(), 
            Instant.now(), 
            1
        );
    }

    void addAsset(Asset asset) {
        // Check by AssetIdentifier - can't have AAPL twice in same account
        boolean alreadyExists = assets.stream()
            .anyMatch(a -> a.getAssetIdentifier().equals(asset.getAssetIdentifier()));
        
        if (alreadyExists) {
            throw new IllegalStateException(
                "Asset with identifier " + asset.getAssetIdentifier().displayName() + 
                " already exists in this account"
            );
        }

        this.assets.add(asset);
        updateMetadata();
    }

    void removeAsset(AssetId assetId) {
        Objects.requireNonNull(assetId, "Asset ID cannot be null");
        
        boolean removed = this.assets.removeIf(a -> a.getAssetId().equals(assetId));
        
        if (removed) {
            updateMetadata();
        }
    }

    // === Cash Management ===
    Money getCashBalance() {
        return assets.stream()
            .filter(asset -> asset.getAssetType() == AssetType.CASH)
            .map(Asset::getCostBasis)
            .reduce(Money.ZERO(baseCurrency), Money::add);
    }

    // they should be private package
    void addCash(Money amount) {
        Optional<Asset> cashAsset = findCashAsset();
        
        if (cashAsset.isPresent()) {
            Asset cash = cashAsset.get();
            // Update quantity and cost basis
            Quantity newQuantity = cash.getQuantity().add(new Quantity(amount.amount()));
            cash.adjustQuantity(newQuantity);
            cash.updateCostBasis(cash.getCostBasis().add(amount));
        } else {
            // Create new cash asset
            Asset newCash = Asset.create(
                new CashAssetIdentifier(UUID.randomUUID(), amount.currency()),
                AssetType.CASH,
                Quantity.ZERO(),
                amount,
                LocalDateTime.now()
            );
            assets.add(newCash);
        }
    }

    public void deductCash(Money amount) {
        Asset cashAsset = findCashAsset()
            .orElseThrow(() -> new InsufficientFundsException("No cash available"));
        
        Money currentCash = cashAsset.getCostBasis();
        if (currentCash.isLessThan(amount)) {
            throw new InsufficientFundsException(
                "Insufficient cash: need " + amount + ", have " + currentCash
            );
        }
        
        Quantity newQuantity = cashAsset.getQuantity().subtract(new Quantity(amount.amount()));
        cashAsset.adjustQuantity(newQuantity);
        cashAsset.updateCostBasis(currentCash.subtract(amount));
        
        if (newQuantity.isZero()) {
            assets.remove(cashAsset);
        }
    }

    // asset management //

    public Asset getAsset(AssetIdentifier assetIdentifier) {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null");
        
        return this.assets.stream()
            .filter(a -> a.getAssetIdentifier().equals(assetIdentifier))
            .findFirst()
            .orElseThrow(() -> new AssetNotFoundException( 
                "Asset " + assetIdentifier.displayName() + " not found in account"
            ));
    }

    public Optional<Asset> findAsset(AssetIdentifier assetIdentifier) {
        return this.assets.stream()
            .filter(x -> x.getAssetIdentifier().equals(assetIdentifier))
            .findFirst();
    }

    public boolean hasAsset(AssetIdentifier assetIdentifier) {
        return this.assets.stream()
            .anyMatch(x -> x.getAssetIdentifier().equals(assetIdentifier));
    }

    public boolean isEmpty() {
        return this.assets.isEmpty();
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public List<Asset> getAssets() {
        return Collections.unmodifiableList(assets);
    }

    public Instant getSystemCreationDate() {
        return systemCreationDate;
    }

    public Instant getLastSystemInteraction() {
        return lastSystemInteraction;
    }

    public int getVersion() {
        return version;
    }

    private void updateMetadata() {
        version++;
        this.lastSystemInteraction = Instant.now();
    }

    private Optional<Asset> findCashAsset() {
        return assets.stream()
            .filter(a -> a.getAssetType() == AssetType.CASH)
            .findFirst();
    }
}
