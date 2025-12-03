package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;

import com.laderrco.fortunelink.shared.valueobjects.Money;

public record PerformanceResponse(Money totalReturns, Money annualizedReturn, Money realizedGains, Money unrealizedGains, Money timeWeightedReturn, Money moneyWeightedReturn, Instant period) {
}