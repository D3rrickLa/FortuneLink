package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;

import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record NetworthResponse(Money totalAssets, Money totalLiabilities, Money netWorth, Instant asOfDate, ValidatedCurrency currency) {
}