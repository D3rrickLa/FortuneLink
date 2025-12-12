package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.util.Currency;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class MarketAssetInfo {
    private final String symbol;
    private final String name;
    private final AssetType assetType;
    private final String exchange;
    private final Currency currency;
    private final String sector;
    private final Money currentPrice;

    public MarketAssetInfo(String symbol, String name, AssetType assetType, String exchange, Currency currency, String sector, Money currentPrice) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.exchange = exchange;
        this.currency = currency;
        this.sector = sector;
        this.currentPrice = currentPrice;
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
    public Currency getCurrency() {
        return currency;
    }
    public String getSector() {
        return sector;
    }
    public Money getCurrentPrice() {
        return currentPrice;
    }    

    
}
