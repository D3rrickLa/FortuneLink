package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.common;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.PriceResponse;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

/**
 * Anti-Corruption Layer mapper.
 * Converts provider models (infrastructure) to domain models.
 * 
 * Responsibilities:
 * 1. Map ProviderQuote → Money (domain value object)
 * 2. Map ProviderAssetInfo → MarketAssetInfo (domain DTO)
 * 3. Normalize symbol formats between providers
 * 4. Handle currency conversions/validations
 */
@Component
public class MarketDataMapper {

    /**
     * Convert provider quote to domain Money value object.
     */
    public Money toMoney(ProviderQuote quote) {
        ValidatedCurrency currency = parseCurrency(quote.currency());
        return new Money(quote.price(), currency);
    }

    /**
     * Convert provider asset info to domain AssetInfo DTO.
     */
    public MarketAssetInfo toAssetInfo(ProviderAssetInfo info) {
        return new MarketAssetInfo(
                normalizeSymbol(info.symbol()),
                info.name(),
                AssetType.valueOf(info.assetType()),
                info.exchange(),
                ValidatedCurrency.of(info.currency()),
                info.sector(),
                info.description());
    }

    /**
     * 
     * @param symbol
     * @param price
     * @return a new PriceResponse object
     */
    public PriceResponse toPriceResponse(String symbol, Money price) {
        return PriceResponse.of(symbol, price.amount(), price.currency().getCode());
    }

    /**
     * Normalize symbol format across different providers.
     * 
     * Examples:
     * - Yahoo: "AAPL", "BTC-USD", "VGRO.TO"
     * - Alpha Vantage: "AAPL", "BTC"
     * - Finnhub: "AAPL", "BINANCE:BTCUSDT"
     * 
     * For MVP, we'll store Yahoo format as canonical.
     */
    public String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null) {
            throw new IllegalArgumentException("Symbol cannot be null");
        }
        return rawSymbol.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Convert domain AssetIdentifier to provider-specific format.
     * 
     * For MVP with Yahoo, this is identity function.
     * When adding other providers, implement provider-specific transformations.
     * This is not going to be useful in the near or long future so we are going to just ignore it and unimpl it
     */
    public String toProviderSymbol(AssetIdentifier symbol, String providerName) {
        // For Yahoo, use as-is
        if ("YAHOO_FINANCE".equals(providerName)) {
            return symbol.getPrimaryId();
        }

        // Future: handle other providers
        // e.g., for Alpha Vantage, might strip ".TO" suffix
        return symbol.getPrimaryId();
    }

    /**
     * Parse currency string to domain Currency enum.
     * Handles common cases and defaults to USD for unknown.
     */
    private ValidatedCurrency parseCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return ValidatedCurrency.USD;
        }

        try {
            return ValidatedCurrency.of(currencyCode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // Currency not in our enum - log warning and default to USD
            // In production, might want to be more strict or expand Currency enum
            return ValidatedCurrency.USD;
        }
    }
}