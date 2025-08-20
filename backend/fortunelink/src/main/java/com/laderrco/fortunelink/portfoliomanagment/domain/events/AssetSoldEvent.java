package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.events.interfaces.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;

public record AssetSoldEvent(
    PortfolioId portfolioId,
    AssetHoldingId assetHoldingId,
    AssetIdentifier assetIdentifier,
    BigDecimal quantityToSell,
    Money grossProceeds,
    Money realizedGainLoss,
    Instant occurredAt
) implements DomainEvent {
    public AssetSoldEvent {
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        Objects.requireNonNull(assetHoldingId, "Asset holding id cannot be null.");
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        Objects.requireNonNull(quantityToSell, "Quantity to sell cannot be null.");
        Objects.requireNonNull(grossProceeds, "Gross proceeds of sold asset cannot be null.");
        Objects.requireNonNull(realizedGainLoss, "Realized gain/loss of sold asset cannot be null.");
        Objects.requireNonNull(occurredAt, "Occurred at of event cannot be null.");
    }
}
