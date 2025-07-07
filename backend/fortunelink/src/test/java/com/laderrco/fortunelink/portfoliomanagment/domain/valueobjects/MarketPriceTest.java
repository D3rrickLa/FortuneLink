package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class MarketPriceTest {
    AssetIdentifier assetIdentifier;
    Money price;
    Instant priceDate;
    String source = "";
    Currency usd;
    Currency cad;
    Currency eur;

    @BeforeEach
    void init() {
        assetIdentifier = new AssetIdentifier(
        AssetType.STOCK, 
        "US0378331005 ",
        "APPLE", 
        "NASDAQ", 
        "SOME DESCRIPTION"
        );
        usd = Currency.getInstance("USD");
        cad = Currency.getInstance("CAD");
        eur = Currency.getInstance("EUR");
        price = new Money(new BigDecimal("213.55"), usd);
        priceDate = Instant.now();
    } 
    
    @Test
    void testConstructor() {
        MarketPrice marketPrice = new MarketPrice(assetIdentifier, price, priceDate, source);
        assertNotNull(marketPrice);
    }

    @Test
    void testConstructorInValidNulls() {
        assertThrows(NullPointerException.class, () ->  new MarketPrice(null, price, priceDate, source));
        assertThrows(NullPointerException.class, () ->  new MarketPrice(assetIdentifier, null, priceDate, source));
        assertThrows(NullPointerException.class, () ->  new MarketPrice(assetIdentifier, price, null, source));
    }

    @Test
    void testConstructorInValidPriceNotPositive() {
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> new MarketPrice(assetIdentifier, price.negate(), priceDate, source));
        assertEquals("Price per unit of asset must be positive.", e1.getMessage());
        e1 = assertThrows(IllegalArgumentException.class, () -> new MarketPrice(assetIdentifier, new Money(BigDecimal.ZERO, usd), priceDate, source));
        assertEquals("Price per unit of asset must be positive.", e1.getMessage());
    }
    
    @Test
    void testGetPriceInCurrency() {
        ExchangeRate exchangeRate = new ExchangeRate(usd, cad, new BigDecimal("1.38"),  Instant.now(), "SOME EXCHANGE SOURCE");
        MarketPrice marketPrice = new MarketPrice(assetIdentifier, price, priceDate, source);

        MarketPrice cadMarketPrice = marketPrice.getPriceInCurrency(cad, exchangeRate);
        BigDecimal expectedCost = new BigDecimal("294.70").setScale(cad.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);

        assertEquals(expectedCost, cadMarketPrice.price().amount());
    }

    @Test
    void testGetPriceInCurrencyInvalidExchangeRate() {
        // Create market price in USD
        MarketPrice usdMarketPrice = new MarketPrice(assetIdentifier, price, priceDate, source);
        
        // Create exchange rate from CAD to EUR (wrong 'from' currency)
        ExchangeRate wrongFromCurrency = new ExchangeRate(cad, eur, new BigDecimal("0.85"), Instant.now(), "SOURCE");
        
        // Should throw exception - exchange rate 'from' doesn't match price currency
        assertThrows(IllegalArgumentException.class, () -> 
            usdMarketPrice.getPriceInCurrency(eur, wrongFromCurrency));
        
        // Create exchange rate from USD to CAD (correct 'from', wrong 'to')
        ExchangeRate wrongToCurrency = new ExchangeRate(usd, cad, new BigDecimal("1.38"), Instant.now(), "SOURCE");
        
        // Should throw exception - exchange rate 'to' doesn't match target currency
        assertThrows(IllegalArgumentException.class, () -> 
            usdMarketPrice.getPriceInCurrency(eur, wrongToCurrency));
    }

    @Test
    void testGetPriceInCurrencyNullParameters() {
        MarketPrice marketPrice = new MarketPrice(assetIdentifier, price, priceDate, source);
        ExchangeRate exchangeRate = new ExchangeRate(usd, cad, new BigDecimal("1.38"), Instant.now(), "SOURCE");
        
        // Test null target currency
        assertThrows(NullPointerException.class, () -> 
            marketPrice.getPriceInCurrency(null, exchangeRate));
        
        // Test null exchange rate
        assertThrows(NullPointerException.class, () -> 
            marketPrice.getPriceInCurrency(cad, null));
    }


    @Test
    void testIsStale() {
        // Test with past date (definitely stale)
        Instant pastDate = Instant.now().minus(Duration.ofHours(25));
        MarketPrice staleMarketPrice = new MarketPrice(assetIdentifier, price, pastDate, source);
        assertTrue(staleMarketPrice.isStale(Duration.ofHours(24)));
        
        // Test with recent date (not stale)
        Instant recentDate = Instant.now().minus(Duration.ofMinutes(5));
        MarketPrice freshMarketPrice = new MarketPrice(assetIdentifier, price, recentDate, source);
        assertFalse(freshMarketPrice.isStale(Duration.ofHours(24)));
    }
}
