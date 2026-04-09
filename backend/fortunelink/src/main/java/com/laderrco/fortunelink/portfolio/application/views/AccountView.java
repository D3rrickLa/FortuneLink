package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import java.time.Instant;
import java.util.List;

public record AccountView(
    AccountId accountId,
    String name,
    AccountType type,
    AccountLifecycleState status,
    List<PositionView> assets,
    Currency baseCurrency,
    Money cashBalance,
    Money totalValue,
    Instant creationDate,
    boolean hasCashImbalance,
    int excludedTransactionCount) {
}