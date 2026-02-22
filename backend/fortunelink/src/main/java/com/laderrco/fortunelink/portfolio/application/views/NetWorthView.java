package com.laderrco.fortunelink.portfolio.application.views;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;

public record NetWorthView(Money totalAssets, Money totalLiabilities, Money netWorth, Currency displayCurrency, Instant asOfDate) {

}