package com.laderrco.fortunelink.portfolio_management.infrastructure.models;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.shared.valueobjects.Money;

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
     * Convert domain Price to MarkteDataController API response.
     */
    public PriceResponse toPriceResponse(String symbol, Money price) {
        return PriceResponse.builder()
                .symbol(symbol)
                .price(price.amount())
                .currency(price.currency().getSymbol())
                .timestamp(Instant.now())
                .source("Yahoo Finance")
                .build();
    }

    /**
     * Convert map of domain Prices to API responses.
     * Used for batch endpoints.
     */
    public Map<String, PriceResponse> toPriceResponseMap(Map<AssetIdentifier, Money> prices) {
        return prices.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getPrimaryId(),
                        entry -> toPriceResponse(entry.getKey().getPrimaryId(), entry.getValue())
                ));
    }

    /**
     * Convert domain AssetInfo to API response.
     */
    public AssetInfoResponse toAssetInfoResponse(MarketAssetInfo assetInfo) {
        return AssetInfoResponse.builder()
                .symbol(assetInfo.getSymbol())
                .name(assetInfo.getName())
                .assetType(assetInfo.getAssetType().toString())
                .currency(assetInfo.getCurrency().getSymbol())
                .exchange(assetInfo.getExchange())
                // .currentPrice(assetInfo.getCurrentPrice()) /TODO might need to pass another var for this additional info...
                .sector(assetInfo.getSector())
                // .marketCap(assetInfo.getMarketCap())
                // .peRatio(assetInfo.getPeRatio())
                // .fiftyTwoWeekHigh(assetInfo.getFiftyTwoWeekHigh())
                // .fiftyTwoWeekLow(assetInfo.getFiftyTwoWeekLow())
                // .averageVolume(assetInfo.getAverageVolume())
                .source("API CALL")
                .build();
    }

    /**
     * Convert map of domain AssetInfo to API responses.
     * Used for batch endpoints.
     */
    public Map<String, AssetInfoResponse> toAssetInfoResponseMap(Map<AssetIdentifier, MarketAssetInfo> assetInfoMap) {
        return assetInfoMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getPrimaryId(),
                        entry -> toAssetInfoResponse(entry.getValue())
                ));
    }
}