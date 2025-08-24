package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.util.Map;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AssetType;

/**
 * This is a general purpose record that will be used in the Asset.java class.
 * It will handle the following
 *  - Traditional securities (Stocks, ETFs, Bonds)
 * - Commodities
 * - Crypto
 * - Real estate
 */
public record AssetIdentifier(
    AssetType type,
    String primaryId,                   // ISIN, contract address, parcel ID, etc.
    Map<String, String> alternativeIds, // ticker, CUSIP, SEDOL, etc. (optional)
    String assetName, 
    String market,                      // exchange, blockchain, registry, etc.
    String unitOfTrade                  // USD, BTC, barrel, sqft
) {
    public AssetIdentifier {
        validateParameter(type, "Asset type");
        
        // assetName, market, and unit of trade must be non-blank, but
        // we might have assets where those are legitimately missing - private real estate property with no "market"
        // solution to that -> PRIVATE as input
        
        primaryId = validatePrimaryId(type, primaryId);
        assetName = validateAndTrim(assetName, "Asset name");
        market = validateAndTrim(market, "Market");
        unitOfTrade = validateAndTrim(unitOfTrade, "Unit of trade");
        alternativeIds = alternativeIds == null ? Map.of() : Map.copyOf(alternativeIds);
    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }
    
    private String validatePrimaryId(AssetType type, String id) {
        id = Objects.requireNonNull(id, "id cannot be null.");
        switch (type) {
            case STOCK, ETF, BOND:
                if (id.isBlank() || !id.matches("^[A-Z]{2}[A-Z0-9]{9}[0-9]$")) {
                    throw new IllegalArgumentException("PrimaryId must be a valid ISIN for securities.");
                }
                break;
            case CRYPTO:
                if (id.isBlank()) {
                    throw new IllegalArgumentException("Crypto must have a contract address or symbol.");
                }
                break;
            default:                
                if (id.isBlank()) {
                    throw new IllegalArgumentException("PrimaryId cannot be blank.");
                }
                break;
        }

        // Only after checks, normalize
        return id.trim().toUpperCase();
    }

    private String validateAndTrim(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.trim();
    }

}