package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import java.time.Instant;

public record NetWorthView(
    Money totalAssets,
    Money totalLiabilities,
    Money netWorth,
    Currency displayCurrency,
    Instant asOfDate) {

}