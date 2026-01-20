package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.util.Map;
import java.util.Objects;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

/**
 * This represents what the Asset Is, immutable values
 */
public class MarketAssetInfo {
    private final String symbol;
    private final String name;
    private final AssetType assetType;
    private final String exchange;
    private final ValidatedCurrency currency; // Trading currency
    private final String sector; // e.g., "Technology", "Finance"
    private final String description;

    public MarketAssetInfo(String symbol, String name, AssetType assetType, String exchange, ValidatedCurrency currency,
            String sector, String description) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.exchange = exchange;
        this.currency = currency;
        this.sector = sector;
        this.description = description;
    }


    public MarketIdentifier toIdentifier() {
        return new MarketIdentifier(
                this.symbol,
                null,
                this.assetType, // STOCK, ETF, CRYPTO, etc.
                this.name, // Full asset name
                this.currency.getSymbol(), // Asset's native currency
                Map.of("Exchange", this.exchange, "Sector", this.sector) // NYSE, NASDAQ, etc. Technology, Finance, etc.
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public String getExchange() {
        return exchange;
    }

    public ValidatedCurrency getCurrency() {
        return currency;
    }

    public String getSector() {
        return sector;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        // 1. Identity check
        if (this == o)
            return true;

        // 2. Null and Class check
        if (o == null || getClass() != o.getClass())
            return false;

        // 3. Cast and field comparison
        MarketAssetInfo that = (MarketAssetInfo) o;
        return Objects.equals(symbol, that.symbol) &&
                Objects.equals(name, that.name) &&
                assetType == that.assetType &&
                Objects.equals(exchange, that.exchange) &&
                Objects.equals(currency, that.currency) &&
                Objects.equals(sector, that.sector) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        // Generates a hash based on all fields to ensure distribution
        return Objects.hash(symbol, name, assetType, exchange, currency, sector, description);
    }

}
