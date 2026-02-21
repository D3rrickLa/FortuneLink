package com.laderrco.fortunelink.portfolio.domain.services;

import java.time.Instant;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;

public interface ExchangeRateService {
    public Optional<ExchangeRate> getRate(Currency from, Currency to); 
    public Money convert(Money amount, Currency targetCurrency, Instant asOfDate); 
    public Money convert(Money amount, Currency targetCurrency);
    public Price convertToPrice(Money price, Currency targetCurrency);
}