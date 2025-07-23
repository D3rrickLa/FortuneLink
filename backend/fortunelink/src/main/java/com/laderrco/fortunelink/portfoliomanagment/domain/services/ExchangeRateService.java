package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.math.BigDecimal;
import java.util.Currency;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public interface ExchangeRateService {
    BigDecimal getExchangeRate(Currency from, Currency to);
    Money convert(Money amount, Currency targetCurrency);
}
