package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common;

import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;

@Component
public class ExchangeRateMapper {

    public ExchangeRate toExchangeRate(ProviderExchangeRate exchangeRate) {
        return new ExchangeRate(
            convert(exchangeRate.fromCurrency()), 
            convert(exchangeRate.toCurrency()),
            exchangeRate.rate(),
            exchangeRate.date().atStartOfDay(ZoneId.of("UTC")).toInstant(),
            exchangeRate.source());

    }


    private ValidatedCurrency convert(String currency) {
        return ValidatedCurrency.of(currency);
    }
}
