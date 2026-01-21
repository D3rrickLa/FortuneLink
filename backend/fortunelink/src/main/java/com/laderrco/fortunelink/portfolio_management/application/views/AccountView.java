package com.laderrco.fortunelink.portfolio_management.application.views;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record AccountView(AccountId accountId, String name, AccountType type, List<AssetView> assets, ValidatedCurrency baseCurrency, Money cashBalance, Money totalValue, Instant createdDate) implements ClassValidation {
    public AccountView { 
        ClassValidation.validateParameter(accountId);
        ClassValidation.validateParameter(name);
        ClassValidation.validateParameter(type);
        ClassValidation.validateParameter(baseCurrency);
        ClassValidation.validateParameter(cashBalance);
        ClassValidation.validateParameter(totalValue);
        ClassValidation.validateParameter(createdDate);
    }
}