package com.laderrco.fortunelink.portfolio.application.views;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;

public record RealizedGainView(String symbol, Money realizedGainLoss, Money costBasisSold,
    Instant occurredAt, boolean isGain) {}