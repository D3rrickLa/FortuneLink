

package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public interface CurrencyConversionService {
    /**
     * 
     * @param from The currency we are converting from
     * @param to The currency we are converting to
     * @return The conversion rate of (from -> to). For example from CAD to USD would return 0.72 
     */
    public BigDecimal getExchangeRate(Currency from, Currency to);
    
    /**
     * Converts a monetary amount to a target currency using the real-time,
     * most up-to-date exchange rate.
     *
     * @param amount The amount to convert.
     * @param targetCurrency The currency to convert to.
     * @return The converted amount in the target currency.
     */    
    public Money convert(Money amount, Currency targetCurrency);

    /**
     * 
     * @param amount The amount to convert
     * @param targetCurrency The currency to convert to
     * @param asOfDate The specific point in time to use for the exchange rate
     * @return the converted amount in the target Currency
     */
    public Money convert(Money amount, Currency targetCurrency, Instant asOfDate);
}