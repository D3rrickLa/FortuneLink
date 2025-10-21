package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.util.Currency;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;

public class Account {
    private final AccountId accountId;
    private String name;
    private AccountType accountType;
    private Currency baseCurrency;
    private List<Asset> assets; // we use Ids outside your aggregate
}
