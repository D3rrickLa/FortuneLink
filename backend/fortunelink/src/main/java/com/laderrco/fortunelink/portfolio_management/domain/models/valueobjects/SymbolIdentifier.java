package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;

/**
 * This is NOT an input like MarketIdnetifier or CyrptoIdentifier, all this does is transfer data to
 * the proper form
 * 
 * If we treat this as an 'Identity', we will be too abstracted causing lots of problems
 * 
 * this is juse a 'please user give me a symbol' app input
 * 
 * We will wnatto map the symbo lto a market or crypto identifier some time later for caching
 * and persistance
 */
public record SymbolIdentifier(String symbol) implements AssetIdentifier {

    public static SymbolIdentifier of(String symbol) {
        return new SymbolIdentifier(symbol);
    }

    @Override
    public String getPrimaryId() {
        return symbol;
    }

    @Override
    public String displayName() {
        return "UNKNOWN, SYMBOL GIVEN ONLY";
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.OTHER;
    }
    
}
