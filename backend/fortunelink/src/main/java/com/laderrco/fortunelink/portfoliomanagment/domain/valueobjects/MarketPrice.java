package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

public record MarketPrice(AssetIdentifier assetIdentifier, Money price, Instant priceDate, String source) {
    public MarketPrice {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        Objects.requireNonNull(price, "Price cannot be null.");
        Objects.requireNonNull(priceDate, "Price date cannot be null.");

        if (price.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price per unit of asset must be positive.");
        }
    }

    public boolean isStale(Duration maxAge) {
        Objects.requireNonNull(maxAge, "Cannot pass null to the 'isStale' method.");
        return priceDate.isBefore(Instant.now().minus(maxAge));
    }   

    public MarketPrice getPriceInCurrency(Currency targetCurrency, ExchangeRate rate) {
        Objects.requireNonNull(targetCurrency, "Cannot pass target currency as null to the 'getPriceInCurrency' method.");
        Objects.requireNonNull(rate, "Cannot pass exchange rate as null to the 'getPriceInCurrency' method.");
        
        // Validate exchange rate matches the conversion
        if (!this.price.currency().equals(rate.fromCurrency())) {
            throw new IllegalArgumentException("Exchange rate 'from' currency must match price currency");
        }
        if (!targetCurrency.equals(rate.toCurrency())) {
            throw new IllegalArgumentException("Exchange rate 'to' currency must match target currency");
        }
        
        Money convertedPrice = this.price.convertTo(targetCurrency, rate);
        return new MarketPrice(this.assetIdentifier, convertedPrice, this.priceDate, this.source);
    }

    public static MarketPrice ZERO(AssetIdentifier assetIdentifier, Currency currency) {
        return new MarketPrice(assetIdentifier, Money.ZERO(currency), Instant.now(), "SYSTEM");
    }

}