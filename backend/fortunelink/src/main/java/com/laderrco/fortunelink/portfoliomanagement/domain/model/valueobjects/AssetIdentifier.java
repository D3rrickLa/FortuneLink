package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

/*
 * A general purpose record that will be used in Asset.java,
 * handling the following:
 *  - Traditional securities 
 *  - Crypto
 *  - Real Estate (future)
 * 
 * 
 * NOTE ^ THIS IDEA IS WRONG. A VO should be/do one specfic thing, and the previous
 * impelmentation does too much
 */
public sealed interface AssetIdentifier permits MarketSymbol, CustomAssetId {
    String displayName();
}