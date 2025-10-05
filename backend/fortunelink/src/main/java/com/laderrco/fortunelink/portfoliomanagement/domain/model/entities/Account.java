package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;

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

    
}
