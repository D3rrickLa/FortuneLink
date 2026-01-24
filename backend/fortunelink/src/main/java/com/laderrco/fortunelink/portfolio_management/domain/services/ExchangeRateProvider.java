package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ProviderExchangeRate;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

public interface ExchangeRateProvider {
    public ProviderExchangeRate getExchangeRate(ValidatedCurrency from, ValidatedCurrency to, Instant asOf) throws JsonMappingException, JsonProcessingException;
}
