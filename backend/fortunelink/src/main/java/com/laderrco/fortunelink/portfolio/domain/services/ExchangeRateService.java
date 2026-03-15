package com.laderrco.fortunelink.portfolio.domain.services;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import java.time.Instant;
import java.util.Optional;

public interface ExchangeRateService {

  Optional<ExchangeRate> getRate(Currency from, Currency to);

  Money convert(Money amount, Currency targetCurrency, Instant asOfDate);

  Money convert(Money amount, Currency targetCurrency);

  Price convertToPrice(Money price, Currency targetCurrency);
}