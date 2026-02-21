package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public record MarketAssetQuote(
        AssetSymbol symbol,
        Price currentPrice,
        Price openPrice,
        Price highPrice,
        Price lowPrice,
        Price previousClose,
        PercentageChange changePercent, // since last market day
        BigDecimal changeAmount,
        BigDecimal marketCap,
        BigDecimal volume,
        String source,
        Instant timestamp) {
}