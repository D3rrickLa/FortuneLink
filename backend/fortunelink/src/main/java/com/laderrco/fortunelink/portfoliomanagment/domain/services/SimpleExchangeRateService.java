package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import com.laderrco.fortunelink.shared.valueobjects.Money;

public class SimpleExchangeRateService implements ExchangeRateService {

    // Store predefined exchange rates for testing
    private final Map<String, BigDecimal> exchangeRates;

    public SimpleExchangeRateService() {
        this.exchangeRates = new HashMap<>();
        // Define your desired test exchange rates from CAD
        // These are completely arbitrary for testing purposes
        exchangeRates.put("CAD_USD", new BigDecimal("0.73")); // 1 CAD = 0.73 USD
        exchangeRates.put("CAD_EUR", new BigDecimal("0.68")); // 1 CAD = 0.68 EUR
        exchangeRates.put("CAD_GBP", new BigDecimal("0.58")); // 1 CAD = 0.58 GBP
        exchangeRates.put("CAD_JPY", new BigDecimal("115.00")); // 1 CAD = 115.00 JPY

        // Also add the reverse rates for conversion if needed (e.g., USD to CAD)
        // Or handle it dynamically in getExchangeRate
        exchangeRates.put("USD_CAD", new BigDecimal("1.37")); // 1 USD = 1.37 CAD (approx 1/0.73)
        exchangeRates.put("EUR_CAD", new BigDecimal("1.47")); // 1 EUR = 1.47 CAD (approx 1/0.68)
        exchangeRates.put("GBP_CAD", new BigDecimal("1.72")); // 1 GBP = 1.72 CAD (approx 1/0.58)
        exchangeRates.put("JPY_CAD", new BigDecimal("0.0087")); // 1 JPY = 0.0087 CAD (approx 1/115.00)

        // Handle same currency conversion
        exchangeRates.put("CAD_CAD", BigDecimal.ONE);
        exchangeRates.put("USD_USD", BigDecimal.ONE);
        exchangeRates.put("EUR_EUR", BigDecimal.ONE);
        exchangeRates.put("GBP_GBP", BigDecimal.ONE);
        exchangeRates.put("JPY_JPY", BigDecimal.ONE);
    }

    @Override
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        if (from.equals(to)) {
            return BigDecimal.ONE; // Exchange rate for same currency is 1
        }

        String key = from.getCurrencyCode() + "_" + to.getCurrencyCode();
        BigDecimal rate = exchangeRates.get(key);

        if (rate == null) {
            // If the direct rate isn't found, try the inverse (e.g., if we have USD_CAD but need CAD_USD)
            String inverseKey = to.getCurrencyCode() + "_" + from.getCurrencyCode();
            BigDecimal inverseRate = exchangeRates.get(inverseKey);
            if (inverseRate != null) {
                // Ensure no division by zero and handle potential precision issues
                return BigDecimal.ONE.divide(inverseRate, 10, RoundingMode.HALF_UP);
            } else {
                throw new IllegalArgumentException("Exchange rate not found for " + from.getCurrencyCode() + " to " + to.getCurrencyCode());
            }
        }
        return rate;
    }

    @Override
    public Money convert(Money amount, Currency targetCurrency) {
        if (amount.currency().equals(targetCurrency)) {
            return amount; // No conversion needed
        }

        BigDecimal rate = getExchangeRate(amount.currency(), targetCurrency);
        // Assuming your Money class has a method to create a new Money object with the converted amount
        // You'll need to define how Money handles multiplication and precision
        BigDecimal convertedAmount = amount.amount().multiply(rate);
        return new Money(convertedAmount, targetCurrency);
    }
    
}
