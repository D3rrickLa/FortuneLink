package com.laderrco.fortunelink.portfolio_management.infrastructure.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * API response object for asset price information.
 * Clean separation between domain (Price value object) and API layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current price information for an asset")
public class PriceResponse {

    @Schema(description = "Asset symbol", example = "AAPL")
    private String symbol;

    @Schema(description = "Current price per unit", example = "150.25")
    private BigDecimal price;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Timestamp when price was retrieved", example = "2025-01-10T15:30:00Z")
    private Instant timestamp;

    @Schema(description = "Data source", example = "Yahoo Finance")
    private String source;

    /**
     * Helper method to create a simple response (most common use case).
     */
    public static PriceResponse of(String symbol, BigDecimal price, String currency) {
        return PriceResponse.builder()
                .symbol(symbol)
                .price(price)
                .currency(currency)
                .timestamp(Instant.now())
                .source("Yahoo Finance")
                .build();
    }
}