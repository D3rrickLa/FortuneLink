package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;

public interface ExchangeRateService {
    public Optional<ExchangeRate> getRate(Currency from, Currency to); 
    public Money convert(Money amount, Currency targetCurrency, Instant asOfDate); 
    public Money convert(Money amount, Currency targetCurrency);
}