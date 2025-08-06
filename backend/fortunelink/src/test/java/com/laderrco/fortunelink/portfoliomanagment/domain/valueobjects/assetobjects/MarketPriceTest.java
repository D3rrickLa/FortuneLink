package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;

public class MarketPriceTest {

    private final Currency USD = Currency.getInstance("USD");
    private final Currency EUR = Currency.getInstance("EUR");

    private final AssetIdentifier asset = new AssetIdentifier(AssetType.STOCK, "AAPL", "US1234567890", "Apple", "NASDAQ");

    private Money money(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    @Test
    void constructor_shouldCreateValidMarketPrice() {
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("100.00"), USD), Instant.now(), "Yahoo");
        assertEquals(asset, mp.assetIdentifier());
        assertEquals(AssetType.STOCK.getDefaultQuantityPrecision(), asset.type().getDefaultQuantityPrecision());
        assertEquals(new BigDecimal("100.00").setScale(DecimalPrecision.MONEY.getDecimalPlaces()), mp.price().amount());
        assertEquals(USD, mp.price().currency());
        assertEquals("Yahoo", mp.source());
    }

    @Test
    void constructor_shouldAllowZeroPriceIfRelaxed() {
        MarketPrice mp = new MarketPrice(asset, money(BigDecimal.ZERO, USD), Instant.now(), null);
        assertEquals(BigDecimal.ZERO.setScale(DecimalPrecision.MONEY.getDecimalPlaces()), mp.price().normalizedForDisplay().amount());
        assertEquals("SYSTEM", mp.source());
    }

    @Test
    void constructor_shouldThrowForNegativePrice() {
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
            new MarketPrice(asset, money(new BigDecimal("-1"), USD), Instant.now(), "InvalidSource")
        );
        assertTrue(ex.getMessage().contains("Price must not be negative."));
    }

    @Test
    void constructor_shouldThrowIfAssetIdentifierIsNull() {
        Exception ex = assertThrows(NullPointerException.class, () ->
            new MarketPrice(null, money(BigDecimal.ONE, USD), Instant.now(), "Yahoo")
        );
        assertTrue(ex.getMessage().contains("Asset identifier cannot be null."));
    }

    @Test
    void constructor_shouldThrowIfPriceIsNull() {
        Exception ex = assertThrows(NullPointerException.class, () ->
            new MarketPrice(asset, null, Instant.now(), "Yahoo")
        );
        assertTrue(ex.getMessage().contains("Price cannot be null."));
    }

    @Test
    void constructor_shouldThrowIfPriceDateIsNull() {
        Exception ex = assertThrows(NullPointerException.class, () ->
            new MarketPrice(asset, money(BigDecimal.ONE, USD), null, "Yahoo")
        );
        assertTrue(ex.getMessage().contains("Price date cannot be null."));
    }

    @Test
    void isStale_shouldReturnTrueIfPriceIsOlderThanMaxAge() {
        Instant fiveHoursAgo = Instant.now().minus(Duration.ofHours(5));
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("100.00"), USD), fiveHoursAgo, "Yahoo");
        assertTrue(mp.isStale(Duration.ofHours(2)));
    }

    @Test
    void isStale_shouldReturnFalseIfPriceIsRecent() {
        Instant oneHourAgo = Instant.now().minus(Duration.ofHours(1));
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("100.00"), USD), oneHourAgo, "Yahoo");
        assertFalse(mp.isStale(Duration.ofHours(2)));
    }

    @Test
    void isStale_shouldThrowIfMaxAgeIsNull() {
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("100.00"), USD), Instant.now(), "Yahoo");
        Exception ex = assertThrows(NullPointerException.class, () -> mp.isStale(null));
        assertTrue(ex.getMessage().contains("Max duration cannot be null."));
    }

    @Test
    void getPriceInCurrency_shouldConvertPriceCorrectly() {
        ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("0.9"), Instant.now());
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("100.00"), USD), Instant.now(), "Yahoo");

        MarketPrice converted = mp.getPriceInCurrency(EUR, rate);
        assertEquals(EUR, converted.price().currency());
        assertEquals(new BigDecimal("90.00").setScale(DecimalPrecision.MONEY.getDecimalPlaces()), converted.price().normalizedForDisplay().amount());
        assertEquals(mp.assetIdentifier(), converted.assetIdentifier());
        assertEquals(mp.timestamp(), converted.timestamp());
    }

    @Test
    void getPriceInCurrency_shouldThrowIfTargetCurrencyIsNull() {
        ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("0.9"), Instant.now());
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("100.00"), USD), Instant.now(), "Yahoo");

        Exception ex = assertThrows(NullPointerException.class, () -> mp.getPriceInCurrency(null, rate));
        assertTrue(ex.getMessage().contains("Target currency cannot be null."));
    }

    @Test
    void getPriceInCurrency_shouldThrowIfRateIsNull() {
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("100.00"), USD), Instant.now(), "Yahoo");

        Exception ex = assertThrows(NullPointerException.class, () -> mp.getPriceInCurrency(EUR, null));
        assertTrue(ex.getMessage().contains("Rate cannot be null."));
    }

    @Test
    void getPriceInCurrency_shouldThrowIfRateFromCurrencyMismatch() {
        ExchangeRate rate = new ExchangeRate(EUR, USD, new BigDecimal("1.1"), Instant.now()); // Wrong direction
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("100.00"), USD), Instant.now(), "Yahoo");

        Exception ex = assertThrows(IllegalArgumentException.class, () -> mp.getPriceInCurrency(EUR, rate));
        assertTrue(ex.getMessage().contains("must match price currency"));
    }

    @Test
    void getPriceInCurrency_shouldThrowIfTargetCurrencyMismatch() {
        ExchangeRate rate = new ExchangeRate(USD, Currency.getInstance("GBP"), new BigDecimal("0.8"), Instant.now());
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("100.00"), USD), Instant.now(), "Yahoo");

        Exception ex = assertThrows(IllegalArgumentException.class, () -> mp.getPriceInCurrency(EUR, rate));
        assertTrue(ex.getMessage().contains("must match target currency"));
    }

    @Test
    void zeroMethod_shouldCreateValidZeroMarketPrice() {
        MarketPrice zero = MarketPrice.ZERO(asset, USD);
        assertEquals(BigDecimal.ZERO.setScale(DecimalPrecision.MONEY.getDecimalPlaces()), zero.price().normalizedForDisplay().amount());
        assertEquals("SYSTEM", zero.source());
        assertEquals(USD, zero.price().currency());
        assertFalse(zero.isStale(Duration.ofSeconds(1))); // Should be fresh
    }

    // Edge Case: Timestamp right at boundary
    @Test
    void isStale_shouldHandleBoundaryCondition() {
        Instant fixedNow = Instant.parse("2025-08-06T23:27:00Z");
        Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

        Instant threshold = fixedNow.minus(Duration.ofSeconds(10));
        MarketPrice mp = new MarketPrice(asset, money(new BigDecimal("50"), USD), threshold, fixedClock, "Yahoo");

        assertFalse(mp.isStale(Duration.ofSeconds(10))); // exactly at boundary
        assertTrue(mp.isStale(Duration.ofSeconds(9)));   // just over boundary

    }
}
