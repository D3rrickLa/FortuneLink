package com.laderrco.fortunelink.portfolio_management.application.views;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;

public record AccountView(
        AccountId accountId,
        String name,
        AccountType type,
        List<PositionView> assets,
        Currency baseCurrency,
        Money cashBalance,
        Money totalValue,
        Instant createdDate) {
    public AccountView {
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(type);
        Objects.requireNonNull(baseCurrency);
        Objects.requireNonNull(cashBalance);
        Objects.requireNonNull(totalValue);
        Objects.requireNonNull(createdDate);
    }
}