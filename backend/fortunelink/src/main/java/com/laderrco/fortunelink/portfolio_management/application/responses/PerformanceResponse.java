package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;

import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public record PerformanceResponse(Percentage totalReturns, Percentage annualizedReturn, Money realizedGains, Money unrealizedGains, Percentage timeWeightedReturn, Money moneyWeightedReturn, String period) {
}