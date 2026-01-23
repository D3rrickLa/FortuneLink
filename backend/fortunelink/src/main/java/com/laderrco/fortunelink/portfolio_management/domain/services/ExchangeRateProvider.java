package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public interface ExchangeRateProvider {
    public BigDecimal getExchangeRate(ValidatedCurrency from, ValidatedCurrency to, Instant asOf);
}
