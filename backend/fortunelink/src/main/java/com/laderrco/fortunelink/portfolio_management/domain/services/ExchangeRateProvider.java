package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ProviderExchangeRate;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public interface ExchangeRateProvider {
    public ProviderExchangeRate getExchangeRate(ValidatedCurrency from, ValidatedCurrency to, Instant asOf);
}
