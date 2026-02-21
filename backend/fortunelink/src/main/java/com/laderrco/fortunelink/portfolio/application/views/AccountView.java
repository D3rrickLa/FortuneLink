package com.laderrco.fortunelink.portfolio.application.views;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;

public record AccountView(
        AccountId accountId,
        String name,
        AccountType type,
        List<PositionView> assets,
        Currency baseCurrency,
        Money cashBalance,
        Money totalValue,
        Instant createdDate) {}