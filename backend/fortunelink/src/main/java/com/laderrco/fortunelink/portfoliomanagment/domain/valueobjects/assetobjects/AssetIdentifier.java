package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;

public record AssetIdentifier(
    AssetType type,
    String symbol,
    String isin,
    String assetName,
    String assetExchangeName,
    String description,
    String sector

    // might need actual metadata -> this willl be in a different context
    /*
     * bid price, asking price, 52W high and lows, MKT cap, etc.
     */
) {
    
}
