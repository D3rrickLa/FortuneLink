package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.YahooQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.yahoo.chart.YahooChartResult;

/**
 * Maps Yahoo Finance API responses to internal provider models.
 * 
 * This is part of the Anti-Corruption Layer (ACL) - isolates the rest
 * of the system from Yahoo's specific response structure.
 * 
 * Responsibilities:
 * 1. Convert YahooQuote → ProviderQuote
 * 2. Convert YahooQuote → ProviderAssetInfo
 * 3. Extract prices from YahooChartResult
 * 4. Handle nulls and missing data gracefully
 */
@Component
public class YahooResponseMapper {
    private static final String SOURCE = "YAHOO";

    /**
     * Convert Yahoo quote to ProviderQuote.
     */
    public ProviderQuote toProviderQuote(YahooQuote quote) {
        return new ProviderQuote(
                quote.getSymbol(),
                quote.getPrice(),
                quote.getCurrencyOrDefault(),
                timestampToLocalDateTime(quote.getTimestampOrNow()),
                SOURCE);
    }

    /**
     * Convert Yahoo quote to ProviderAssetInfo.
     */
    public ProviderAssetInfo toProviderAssetInfo(YahooQuote quote) {
        String name = quote.getLongName() != null ? quote.getLongName() : quote.getShortName();
        String assetType = mapQuoteTypeToAssetType(quote.getQuoteType());

        return new ProviderAssetInfo(
                quote.getSymbol(),
                name,
                quote.getLongName(), // description
                assetType,
                quote.getExchange(),
                quote.getCurrencyOrDefault(),
                SOURCE);
    }

    /**
     * Extract closest price from chart data to target date.
     * Returns the price data point closest to the target LocalDateTime.
     */
    public Optional<ProviderQuote> extractClosestPrice(
            YahooChartResult chartResult,
            LocalDateTime targetDate,
            String symbol) {
        if (!chartResult.hasData()) {
            return Optional.empty();
        }

        var timestamps = chartResult.getTimestamp();
        var quote = chartResult.getIndicators().getFirstQuote();

        if (quote == null || !quote.hasClosePrices()) {
            return Optional.empty();
        }

        // Find index of timestamp closest to target
        int closestIndex = findClosestTimestampIndex(timestamps, targetDate);

        // Get price at that index
        BigDecimal price = quote.getCloseAt(closestIndex);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        // Get actual timestamp for that price
        LocalDateTime actualTime = timestampToLocalDateTime(timestamps.get(closestIndex));

        // Get currency from metadata
        String currency = chartResult.isMeta() != false
                ? chartResult.getCurrencyOrDefault()
                : "USD";

        return Optional.of(new ProviderQuote(
                symbol,
                price,
                currency,
                actualTime,
                SOURCE));
    }

    // --- Private Helper Methods ---

    /**
     * Find index of timestamp closest to target date.
     */
    private int findClosestTimestampIndex(List<Long> timestamps, LocalDateTime target) {
        long targetEpoch = target.atZone(ZoneId.systemDefault()).toEpochSecond();

        int closestIndex = 0;
        long minDiff = Math.abs(timestamps.get(0) - targetEpoch);

        for (int i = 1; i < timestamps.size(); i++) {
            long diff = Math.abs(timestamps.get(i) - targetEpoch);
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }

        return closestIndex;
    }

    /**
     * Convert Unix timestamp (seconds) to LocalDateTime.
     */
    private LocalDateTime timestampToLocalDateTime(long epochSeconds) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSeconds),
                ZoneId.systemDefault());
    }

    /**
     * Map Yahoo's quoteType to our standard asset types.
     * 
     * Yahoo quote types:
     * - EQUITY → STOCK
     * - ETF → ETF
     * - CRYPTOCURRENCY → CRYPTO
     * - MUTUALFUND → MUTUAL_FUND
     * - INDEX → INDEX
     */
    private String mapQuoteTypeToAssetType(String quoteType) {
        if (quoteType == null) {
            return "UNKNOWN";
        }

        return switch (quoteType.toUpperCase()) {
            case "EQUITY" -> "STOCK";
            case "ETF" -> "ETF";
            case "CRYPTOCURRENCY" -> "CRYPTO";
            case "MUTUALFUND" -> "MUTUAL_FUND";
            case "INDEX" -> "INDEX";
            case "CURRENCY" -> "CURRENCY";
            default -> quoteType.toUpperCase();
        };
    }
}
