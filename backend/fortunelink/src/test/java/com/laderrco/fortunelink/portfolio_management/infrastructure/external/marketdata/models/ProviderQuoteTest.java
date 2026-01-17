package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.models;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ProviderQuote Unit Tests")
class ProviderQuoteTest {

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    @DisplayName("Should create valid ProviderQuote when all fields are correct")
    void shouldCreateValidQuote() {
        ProviderQuote quote = new ProviderQuote("AAPL", new BigDecimal("150.00"), "USD", now, "YAHOO");

        assertThat(quote.symbol()).isEqualTo("AAPL");
        assertThat(quote.price()).isEqualByComparingTo("150.00");
        assertThat(quote.currency()).isEqualTo("USD");
        assertThat(quote.timestamp()).isEqualTo(now);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("Should throw exception if symbol is blank or null")
    void shouldThrowExceptionForInvalidSymbol(String invalidSymbol) {
        assertThatThrownBy(() -> new ProviderQuote(invalidSymbol, BigDecimal.TEN, "USD", now, "YAHOO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Symbol cannot be null or blank");

        assertThatThrownBy(() -> new ProviderQuote(null, BigDecimal.TEN, "USD", now, "YAHOO"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception if price is zero or negative")
    void shouldThrowExceptionForInvalidPrice() {
        // Zero price
        assertThatThrownBy(() -> new ProviderQuote("AAPL", BigDecimal.ZERO, "USD", now, "YAHOO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price must be positive");

        // Negative price
        assertThatThrownBy(() -> new ProviderQuote("AAPL", new BigDecimal("-1.00"), "USD", now, "YAHOO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price must be positive");
        
        // Null price
        assertThatThrownBy(() -> new ProviderQuote("AAPL", null, "USD", now, "YAHOO"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception if currency is blank")
    void shouldThrowExceptionForInvalidCurrency() {
        assertThatThrownBy(() -> new ProviderQuote("AAPL", BigDecimal.TEN, "", now, "YAHOO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception if currency is null")
    void shouldThrowExceptionForInvalidCurrencyNullCheck() {
        assertThatThrownBy(() -> new ProviderQuote("AAPL", BigDecimal.TEN, null, now, "YAHOO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception if timestamp is null")
    void shouldThrowExceptionForNullTimestamp() {
        assertThatThrownBy(() -> new ProviderQuote("AAPL", BigDecimal.TEN, "USD", null, "YAHOO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Timestamp cannot be null");
    }
}