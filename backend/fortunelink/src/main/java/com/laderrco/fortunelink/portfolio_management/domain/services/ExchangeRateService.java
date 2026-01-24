package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyAreTheSameException;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public interface ExchangeRateService {
    public Optional<ExchangeRate> getExchangeRate(ValidatedCurrency from, ValidatedCurrency to) throws CurrencyAreTheSameException, JsonMappingException, JsonProcessingException; 
    public Money convert(Money amount, ValidatedCurrency targetCurrency, Instant asOfDate) throws JsonMappingException, JsonProcessingException; 
    public Money convert(Money amount, ValidatedCurrency targetCurrency)  throws JsonMappingException, JsonProcessingException; // right now 
}
