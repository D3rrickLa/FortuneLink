package com.laderrco.fortunelink.portfolio_management.api.models.market;

import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetQuote;

/**
 * Maps between domain objects and API DTOs for market data.
 * 
 * Separation of Concerns:
 * - Domain objects (Price, AssetInfo) = internal business logic
 * - DTOs (PriceResponse, AssetInfoResponse) = external API contracts
 * 
 * This allows domain to evolve independently from API contracts.
 */
@Component
public class MarketDataDtoMapper {
    /**
     * Convert domain AssetInfo to API response via MarketAssetInfo + optional
     * MarketAssetQuote to API DTO.
     */
    public AssetInfoResponse toAssetInfoResponse(MarketAssetInfo info, MarketAssetQuote quote) {
        return AssetInfoResponse.builder()
                .symbol(info.getSymbol())
                .name(info.getName())
                .assetType(info.getAssetType().toString())
                .currency(info.getCurrency().getCode())
                .exchange(info.getExchange())
                .sector(info.getSector())
                .description(info.getDescription())
                .currentPrice(quote != null ? quote.currentPrice().amount() : null)
                .marketCap(quote != null ? quote.marketCap() : null)
                // .peRatio(assetInfo.getPeRatio())
                // .fiftyTwoWeekHigh(assetInfo.getFiftyTwoWeekHigh())
                // .fiftyTwoWeekLow(assetInfo.getFiftyTwoWeekLow())
                // .averageVolume(assetInfo.getAverageVolume())
                .timestamp(quote != null ? quote.lastUpdated() : null)
                .source(quote != null ? quote.source() : null)
                .build();
    }

    /**
     * Convert domain Price to MarkteDataController API response.
     */
    public PriceResponse toPriceResponse(String symbol, MarketAssetQuote quote) {
        return PriceResponse.builder()
                .symbol(symbol)
                .price(quote.currentPrice().amount())
                .currency(quote.currentPrice().currency().getCode())
                .timestamp(Instant.now())
                .source(quote.source()) // we need a way to pass info to this
                .build();
    }

    /**
     * Convert map of domain Prices to API responses.
     * Used for batch endpoints.
     */
    public Map<String, PriceResponse> toPriceResponseMap(Map<AssetIdentifier, MarketAssetQuote> prices) {
        return prices.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getPrimaryId(),
                        entry -> toPriceResponse(entry.getKey().getPrimaryId(), entry.getValue())));
    }

    /**
     * Convert map of domain AssetInfo to API responses.
     * Used for batch endpoints.
     */
    public Map<String, AssetInfoResponse> toAssetInfoResponseMap(
            Map<AssetIdentifier, MarketAssetInfo> infoMap,
            Map<AssetIdentifier, MarketAssetQuote> quoteMap) {

        Map<String, MarketAssetQuote> quotesByKey = quoteMap == null
                ? Map.of()
                : quoteMap.values().stream()
                        .collect(Collectors.toMap(
                                q -> q.id().cacheKey(),
                                Function.identity(),
                                (a, b) -> a));

        return infoMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getPrimaryId(),
                        entry -> toAssetInfoResponse(
                                entry.getValue(),
                                quotesByKey.get(entry.getKey().cacheKey())),
                        (existing, replacement) -> existing));
    }
}