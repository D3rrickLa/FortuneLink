package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;

public record DividendReceivedEvent(
    AssetIdentifier assetIdentifier,
    Money amount, 
    Instant timestamp
) {
    
}
