package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.shared.domain.valueobjects.Money;

public class Account {
    private final AccountId accountId;
    private String name;
    private String type; // need to make an account type
    private final Currency baseCurrency;
    private final List<AssetHolding> assets;
    
    public Account(AccountId accountId, String name, String type, Currency baseCurrency) {
        this.accountId = accountId;
        this.name = name;
        this.type = type;
        this.baseCurrency = baseCurrency;
        this.assets = new ArrayList<>();
    }

    public void addAssetHolding(AssetHolding asset) {

    }

    public void removeAsset(AssetHoldingId assetHoldingId) {

    }

    public Optional<AssetHolding> getAssetHolding(AssetIdentifier identifier) {
        return null;
    }

    public Money calculateTotalValue() {
        return null;
    }
    
}
