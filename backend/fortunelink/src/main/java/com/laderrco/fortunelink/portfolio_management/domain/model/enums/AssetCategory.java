package com.laderrco.fortunelink.portfolio_management.domain.model.enums;

/**
 * High-level classification of financial assets.
 */
public enum AssetCategory {
    /** Publicly traded equity instruments (stocks, ETFs, funds). */
    EQUITY,

    /** Cryptographically secured digital assets (e.g., Bitcoin, Ethereum). */
    CRYPTOCURRENCY,

    /** Fiat cash holdings (checking, savings). */
    CASH,

    /** Bonds and interest-bearing debt instruments. */
    FIXED_INCOME,

    /** Highly liquid, low-risk instruments (MMF, T-bills). */
    CASH_EQUIVALENT,

    /** Physical or derivative commodities (gold, oil, wheat). */
    COMMODITY,

    /** Real estate and other tangible assets. */
    REAL_ASSET,

    /** Fallback category for uncategorized assets. */
    OTHER
}