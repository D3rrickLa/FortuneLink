package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode // TODO: make your own
public class MarketAssetInfo {
    private final String symbol;
    private final String name;
    private final AssetType assetType;
    private final String exchange;
    private final ValidatedCurrency currency;
    private final String sector;
    private final Money currentPrice;

    public MarketAssetInfo(String symbol, String name, AssetType assetType, String exchange, ValidatedCurrency currency, String sector, Money currentPrice) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.exchange = exchange;
        this.currency = currency;
        this.sector = sector;
        this.currentPrice = currentPrice;
    }

    public MarketIdentifier toIdentifier() {
        return new MarketIdentifier(
            this.symbol,
            null,
            this.assetType,             // STOCK, ETF, CRYPTO, etc.
            this.name,                  // Full asset name
            this.currency.getSymbol(),  // Asset's native currency
            Map.of("Exchange", this.exchange, "Sector", this.sector)  // NYSE, NASDAQ, etc. Technology, Finance, etc.
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
    public Money getCurrentPrice() {
        return currentPrice;
    }        
}
