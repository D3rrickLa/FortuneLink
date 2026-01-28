package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

class ExchangeRateMapperTest {

    private final ExchangeRateMapper mapper = new ExchangeRateMapper();

    @Test
    void toExchangeRate_ShouldMapAllFieldsCorrectly() {
        // Arrange
        LocalDate date = LocalDate.of(2023, 10, 27);
        ProviderExchangeRate providerRate = new ProviderExchangeRate(
            "USD", 
            "CAD", 
            new BigDecimal("1.385000"), 
            date, 
            "Bank of Canada"
        );

        // Act
        ExchangeRate result = mapper.toExchangeRate(providerRate);

        // Assert
        assertEquals("USD", result.from().getCode());
        assertEquals("CAD", result.to().getCode());
        assertEquals(new BigDecimal("1.385000"), result.rate());
        assertEquals("Bank of Canada", result.source());
        
        // Check Instant conversion: Oct 27 2023 at 00:00:00 UTC
        Instant expectedInstant = Instant.parse("2023-10-27T00:00:00Z");
        assertEquals(expectedInstant, result.exchangeRateDate());
    }

    @Test
    void toMoney_ShouldMultiplyAmountByRate() {
        // Arrange
        Money originalAmount = new Money(new BigDecimal("100.00"), ValidatedCurrency.of("USD"));
        ProviderExchangeRate providerRate = new ProviderExchangeRate(
            "USD", 
            "CAD", 
            new BigDecimal("1.35"), 
            LocalDate.now(), 
            "BOC"
        );

        // Act
        Money result = mapper.toMoney(originalAmount, providerRate);

        // Assert
        // 100.00 * 1.35 = 135.00
        // Note: Using compareTo for BigDecimal to ignore scale differences
        assertEquals(0, new BigDecimal("135.0000").compareTo(result.amount()));
        assertEquals("CAD", result.currency().getCode());
    }
}