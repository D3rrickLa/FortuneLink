package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfoliomanagement.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagement.domain.services.MarketDataService;
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

    public Asset getAsset(AssetIdentifier assetIdentifier) {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null");
        
        return this.assets.stream()
            .filter(a -> a.getAssetIdentifier().equals(assetIdentifier))
            .findFirst()
            .orElseThrow(() -> new AssetNotFoundException( 
                "Asset " + assetIdentifier.displayName() + " not found in account"
            ));
    }

    public Money calculateTotalValue(MarketDataService marketDataService, ExchangeRateService exchangeRateService) {
        // the market data, we would need to stream in each 'arraylist item' into it
        // and sum up the value, but also another point is that we could hold different currenies/assets in different
        // currencies, so this is a weird one
        return null;
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
        return assets;
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
}
