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
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfoliomanagement.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.Currency;
import com.laderrco.fortunelink.shared.exception.CurrencyMismatchException;
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

    // safe lookup
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

    public Money getCashBalance() {
        return assets.stream()
            .filter(asset -> asset.getAssetType() == AssetType.CASH)
            .map(asset -> asset.calculateCurrentValue(Price.of(asset.getCostBasis()))) // For cash, current value = cost basis
            .reduce(Money.ZERO(baseCurrency), Money::add);
    }
    
    public void addCash(Money amount) {
        Objects.requireNonNull(amount, "amount required");
        
        if (!amount.currency().equals(baseCurrency)) {
            throw new CurrencyMismatchException(
                "Cannot add " + amount.currency() + " to account with base currency " + baseCurrency
            );
        }
        
        // Find or create cash asset
        Optional<Asset> cashAsset = assets.stream()
            .filter(a -> a.getAssetType() == AssetType.CASH)
            .filter(a -> a.getAssetIdentifier().displayName().equals(baseCurrency.toString()))
            .findFirst();
        
        if (cashAsset.isPresent()) {
            // Update existing cash holding
            Asset cash = cashAsset.get();
            // Cash has quantity of 1 per currency unit, so quantity = amount
            Quantity additionalQuantity = new Quantity(amount.amount());
            cash.adjustQuantity(additionalQuantity, amount);
        } else {
            // Create new cash asset
            AssetIdentifier cashIdentifier = new CashAssetIdentifier(UUID.randomUUID(), baseCurrency);
            Quantity cashQuantity = new Quantity(amount.amount());
            Asset newCash = Asset.create(
                cashIdentifier,
                AssetType.CASH,
                cashQuantity,
                amount,
                LocalDateTime.now()
            );
            assets.add(newCash);
        }
    }
    
    public void deductCash(Money amount) {
        Objects.requireNonNull(amount, "amount required");
        
        Money currentCash = getCashBalance();
        if (currentCash.isLessThan(amount)) {
            throw new InsufficientFundsException(
                "Cannot deduct " + amount + " from account '" + name + 
                "'. Only " + currentCash + " available"
            );
        }
        
        Asset cashAsset = assets.stream()
            .filter(a -> a.getAssetType() == AssetType.CASH)
            .filter(a -> a.getAssetIdentifier().symbol().equals(baseCurrency.toString()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Cash asset not found"));
        
        Quantity deductQuantity = Quantity.of(amount.amount());
        cashAsset.reduceQuantity(deductQuantity, amount);
        
        // Remove cash asset if balance is zero
        if (cashAsset.getQuantity().isZero()) {
            assets.remove(cashAsset);
        }
    }
    
    public boolean hasAsset(AssetIdentifier assetIdentifier) {
        return assets.stream()
            .anyMatch(a -> a.getAssetIdentifier().equals(assetIdentifier));
    }
    
    public Money calculateTotalValue(MarketDataService marketDataService, Currency targetCurrency) {
        return assets.stream()
            .map(asset -> {
                if (asset.getAssetType() == AssetType.CASH) {
                    // Cash value is straightforward
                    Money cashValue = Money.of(asset.getQuantity().amount(), baseCurrency);
                    return cashValue.convertTo(targetCurrency, marketDataService);
                } else {
                    // Get current market price and calculate value
                    Price currentPrice = marketDataService.getCurrentPrice(asset.getAssetIdentifier());
                    Money assetValue = asset.calculateCurrentValue(currentPrice);
                    return assetValue.convertTo(targetCurrency, marketDataService);
                }
            })
            .reduce(Money.ZERO(targetCurrency), Money::add);
    }
    
    public boolean isEmpty() {
        return assets.isEmpty();
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
}
