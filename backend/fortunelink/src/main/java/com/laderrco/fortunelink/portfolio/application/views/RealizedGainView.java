package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import java.time.Instant;

public record RealizedGainView(
    String symbol,
    Money realizedGainLoss,
    Money costBasisSold,
    Instant occurredAt,
    boolean isGain) {
}