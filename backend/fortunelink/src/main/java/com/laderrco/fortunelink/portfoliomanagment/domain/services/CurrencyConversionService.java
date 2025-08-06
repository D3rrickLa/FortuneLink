

package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.math.BigDecimal;
import java.util.Currency;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public interface CurrencyConversionService {
    public BigDecimal getExchangeRate(Currency from, Currency to);
    public Money convert(Money amount, Currency targetCurrency);
}