package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.math.BigDecimal;
import java.time.Instant;
/**
 * Represents a real-time or delayed price snapshot for a tradable asset. This record
 * encapsulates the current market state, including daily price action, volatility
 * (high/low), and valuation metrics like market capitalization and volume.
 *
 * @param symbol         The unique ticker symbol of the asset.
 * @param currentPrice   The most recent traded price in the market.
 * @param openPrice      The price at which the asset first traded during the current session.
 * @param highPrice      The highest price recorded during the current trading session.
 * @param lowPrice       The lowest price recorded during the current trading session.
 * @param previousClose  The final trading price from the last completed market day.
 * @param changePercent  The relative performance since the last market close (e.g., +2.5%).
 * @param changeAmount   The absolute nominal difference between the current price and previous close.
 * @param marketCap      The total market value of the entity's outstanding shares.
 * @param volume         The total number of units or shares traded during the current session.
 * @param source         The provider or data vendor from which the quote was retrieved.
 * @param timestamp      The specific moment in time when this price snapshot was captured.
 */
public record MarketAssetQuote(
    AssetSymbol symbol,
    Price currentPrice,
    Price openPrice,
    Price highPrice,
    Price lowPrice,
    Price previousClose,
    PercentageChange changePercent,
    BigDecimal changeAmount,
    BigDecimal marketCap,
    BigDecimal volume,
    String source,
    Instant timestamp) {
}