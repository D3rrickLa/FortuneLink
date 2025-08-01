package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

// used by services, not stored in AssetHolding
public record MarketPrice(
    AssetIdentifier assetIdentifier,
    Money price, // asset price in its native currency
    Instant priceDate,
    String source
) {
    public MarketPrice {
        validateParameter(assetIdentifier, "Asset Identifier");
        validateParameter(price, "Price");
        validateParameter(priceDate, "Price Date");

        if (price.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must not be negative.");
        }

        source = source == null ? "SYSTEM": source.trim(); // this might be a problem if blank
    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }

    public MarketPrice getPriceInCurrency(Currency targetCurrency, ExchangeRate rate) {
        validateParameter(targetCurrency, "Target Currency");
        validateParameter(rate, "Rate");
        
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
    public boolean isStale(Duration maxAge) {
        validateParameter(maxAge, "Max Age");
        return priceDate.isBefore(Instant.now().minus(maxAge));
    }   
    
    public static MarketPrice ZERO(AssetIdentifier assetIdentifier, Currency currency) {
        return new MarketPrice(assetIdentifier, Money.ZERO(currency), Instant.now(), "SYSTEM");
    }
}
