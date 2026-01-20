package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.shared.valueobjects.Money;

public record MarketAssetQuote(
        AssetIdentifier id,
        Money currentPrice,
        BigDecimal marketCap,
        BigDecimal changePercent,
        Instant lastUpdated,
        String source // e.g., "FMP", "Yahoo", etc.
) {
    
}
