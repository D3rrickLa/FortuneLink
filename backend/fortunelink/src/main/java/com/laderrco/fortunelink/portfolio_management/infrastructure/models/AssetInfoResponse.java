package com.laderrco.fortunelink.portfolio_management.infrastructure.models;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API response object for detailed asset information.
 * Contains metadata about the asset (name, type, exchange, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed information about an asset")
public class AssetInfoResponse {

    @Schema(description = "Asset symbol", example = "AAPL")
    private String symbol;

    @Schema(description = "Full company/asset name", example = "Apple Inc.")
    private String name;

    @Schema(description = "Asset type", example = "STOCK", allowableValues = {"STOCK", "ETF", "CRYPTO", "BOND"})
    private String assetType;

    @Schema(description = "Trading currency", example = "USD")
    private String currency;

    @Schema(description = "Exchange where asset is traded", example = "NASDAQ")
    private String exchange;

    @Schema(description = "Current price", example = "150.25")
    private BigDecimal currentPrice;

    @Schema(description = "Market sector (for stocks)", example = "Technology")
    private String sector;

    @Schema(description = "Market cap in billions", example = "2500.50")
    private BigDecimal marketCap;

    @Schema(description = "Price-to-Earnings ratio", example = "28.5")
    private BigDecimal peRatio;

    @Schema(description = "52-week high", example = "180.00")
    private BigDecimal fiftyTwoWeekHigh;

    @Schema(description = "52-week low", example = "120.00")
    private BigDecimal fiftyTwoWeekLow;

    @Schema(description = "Average trading volume", example = "75000000")
    private Long averageVolume;

    @Schema(description = "Data source", example = "Yahoo Finance")
    private String source;
}