package com.laderrco.fortunelink.portfoliomanagement.domain.services;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.sharedkernel.exceptions.ExchangeRateNotFoundException;

// FOR TESTING
public class SimpleExchangeRateService implements ExchangeRateService {
    private final Map<String, BigDecimal> rates = new HashMap<>();

    public SimpleExchangeRateService() {
        // Example: 1 USD = 1.35 CAD
        // Store as FROM_TO, e.g., "USD_CAD"
        rates.put("USD_CAD", new BigDecimal("1.35"));
        rates.put("CAD_USD", BigDecimal.ONE.divide(new BigDecimal("1.35"), MathContext.DECIMAL128));
        // Add more as needed
    }

    @Override
    public BigDecimal getCurrencyExchangeRate(Currency fromCurrency, Currency toCurrency, Instant atInstant) {
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        String key = fromCurrency.getCurrencyCode() + "_" + toCurrency.getCurrencyCode();
        if (rates.containsKey(key)) {
            return rates.get(key);
        }
        // For a real system, you'd fetch from a database, external API, etc.
        throw new ExchangeRateNotFoundException("Exchange rate not found for " + fromCurrency + " to " + toCurrency + " at " + atInstant);
    }
}
