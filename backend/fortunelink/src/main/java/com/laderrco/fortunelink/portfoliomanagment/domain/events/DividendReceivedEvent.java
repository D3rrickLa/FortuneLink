package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;

public record DividendReceivedEvent(
    AssetIdentifier assetIdentifier,
    Money amount, 
    Instant timestamp
) {
    public DividendReceivedEvent {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(timestamp, "timestamp of event cannot be null.");
    }
}
