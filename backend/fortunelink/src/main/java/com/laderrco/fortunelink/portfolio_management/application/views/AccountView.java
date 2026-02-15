package com.laderrco.fortunelink.portfolio_management.application.views;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

import lombok.Builder;

@Builder
public record AccountView(AccountId accountId, String name, AccountType type, List<AssetView> assets,
        Currency baseCurrency, Money cashBalance, Money totalValue, Instant createdDate) implements ClassValidation {
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