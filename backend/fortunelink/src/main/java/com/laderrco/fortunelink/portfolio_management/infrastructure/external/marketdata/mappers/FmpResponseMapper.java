package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.ProviderQuote;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.financial_modeling_prep.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models.dtos.financial_modeling_prep.FmpQuoteResponse;

/**
 * Maps FMP API responses to internal provider models.
 * 
 * This is part of the Anti-Corruption Layer (ACL) - isolates the rest
 * of the system from FMP's specific response structure.
 * 
 * Responsibilities:
 * 1. Convert FmpQuoteResponse → ProviderQuote
 * 2. Convert FmpQuoteResponse → ProviderAssetInfo
 * 3. Extract prices from YahooChartResult
 * 4. Handle nulls and missing data gracefully
 */
@Component
public class FmpResponseMapper {
    private static final String SOURCE = "Financial Modeling Prep";

    public ProviderQuote tProviderQuote(FmpQuoteResponse quoteResponse) {
        return new ProviderQuote(
            quoteResponse.getSymbol(), 
            quoteResponse.getPrice(), 
            inferFromExchange(quoteResponse.getExchange()),
            timestampToLocalDateTime(quoteResponse.getTimestamp()),
            SOURCE
        );
    }

    public ProviderAssetInfo toProviderAssetInfo(FmpProfileResponse profileResponse) {
        return new ProviderAssetInfo(
            profileResponse.getSymbol(),
            profileResponse.getCompanyName(),
            profileResponse.getDescription(),
            mapQuoteTypeToAssetType(profileResponse),
            profileResponse.getExchange(),
            profileResponse.getCurrency(),
            profileResponse.getSector(),
            SOURCE
        );
    }

    /**
     * Convert Unix timestamp (seconds) to LocalDateTime.
     */
    private LocalDateTime timestampToLocalDateTime(long epochSeconds) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSeconds),
                ZoneId.systemDefault());
    }

    private String mapQuoteTypeToAssetType(FmpProfileResponse profileResponse) {
        if (profileResponse.getIsEtf()) {
            return "ETF";
        }

        if (profileResponse.getIsFund()) {
            return "MUTUAL_FUND";
        }

        if (profileResponse.getExchange() != null)  {
            String ex = profileResponse.getExchange().toUpperCase();
            if (ex.contains("CRYPTO")) return "CRYPTO";
            if (ex.contains("FOREX") || ex.contains("CURRENCY")) return "CURRENCY";
        }
        // 3. Check Industry/Sector for Indices
        if (profileResponse.getIndustry() != null && profileResponse.getIndustry().toUpperCase().contains("INDEX")) {
            return "INDEX";    
        }

        return "STOCK";
    }

    private String inferFromExchange(String exchange) {
        if (exchange == null) {
            return "USD";
        }

        String ex = exchange.toUpperCase();

        // direct class match - REALLY DIRTY
        if (ex.contains("CRYPTO"))    return "USD";
        if (ex.contains("FOREX"))     return "USD";
        if (ex.contains("COMMODITY")) return "USD";
        if (ex.contains("INDEX"))     return "USD";

        return switch(ex) {
            case "TSX", "TORONTO", "TSXV"   -> "CAD";
            case "LSE", "LONDON"            -> "GBP";
            case "XETRA", "EURONEXT", "MCX" -> "EUR";
            case "HKSE"                     -> "HKD";
            case "ASX"                      -> "AUD";
            case "NSE", "BSE"               -> "INR";
            case "JPX", "TOKYO"             -> "JPY";
            // Add more as needed
            default -> "USD"; // Default for NASDAQ/NYSE 
        };
    }

}
