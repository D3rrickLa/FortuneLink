package com.laderrco.fortunelink.portfoliomanagement.domain.services;

import com.laderrco.fortunelink.shared.enums.Currency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public interface ExchangeRateService {
    ExchangeRate getExchangeRate(Currency from, Currency to);
    Money convert(Money amount, Currency targetCurrency);
}
