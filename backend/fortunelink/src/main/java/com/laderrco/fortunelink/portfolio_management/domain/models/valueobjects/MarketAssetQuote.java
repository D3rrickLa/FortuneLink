package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common.ProviderQuote;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record MarketAssetQuote(
        AssetIdentifier id,
        Money currentPrice,
        Money openPrice,
        Money highPrice,
        Money lowPrice,
        Money previousClose,
        BigDecimal changePercent,
        BigDecimal changeAmount,
        BigDecimal marketCap,
        BigDecimal volume,
        Instant lastUpdated,
        String source // e.g., "FMP", "Yahoo", etc.
) {
    public MarketAssetQuote {
        Objects.requireNonNull(id, "AssetIdentifier cannot be null");
        Objects.requireNonNull(currentPrice, "Current price cannot be null");
        Objects.requireNonNull(lastUpdated, "Last updated timestamp cannot be null");
        Objects.requireNonNull(source, "Source cannot be null");
    }
    
    // for positional constructor, will avoid erros
    public static MarketAssetQuote fromProvider(AssetIdentifier id, ProviderQuote quote) {
        return new MarketAssetQuote(
                id,
                Money.of(quote.price(), ValidatedCurrency.of(quote.currency())),
                null,
                null,
                null,
                null,
                quote.changePercent(),
                null,
                quote.marketCap(),
                null,
                quote.timestamp().toInstant(ZoneOffset.UTC),
                quote.source());
    }

    public BigDecimal computeChangePercent() {
        if (previousClose == null || previousClose.amount().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return currentPrice.amount()
                .subtract(previousClose.amount())
                .divide(previousClose.amount(), Precision.CASH.getDecimalPlaces(), Rounding.MONEY.getMode())
                .multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal computeChangeAmount() {
        if (previousClose == null)
            return null;
        return currentPrice.amount().subtract(previousClose.amount());
    }
}