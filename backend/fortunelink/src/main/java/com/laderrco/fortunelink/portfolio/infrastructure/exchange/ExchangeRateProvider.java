package com.laderrco.fortunelink.portfolio.infrastructure.exchange;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import java.time.Instant;

// NOTE: @Primary is on BOC
public interface ExchangeRateProvider {
  ExchangeRate getExchangeRate(Currency from, Currency to, Instant asOf);
}