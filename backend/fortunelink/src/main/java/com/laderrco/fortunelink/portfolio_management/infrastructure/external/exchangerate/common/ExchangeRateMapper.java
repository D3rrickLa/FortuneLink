package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common;

import java.math.BigDecimal;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

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

    public Money toMoney(Money originalAmount, ProviderExchangeRate exchangeRate) {
        BigDecimal converted = originalAmount.amount().multiply(exchangeRate.rate());
        return new Money(converted, convert(exchangeRate.toCurrency()));
    }

    private ValidatedCurrency convert(String currency) {
        return ValidatedCurrency.of(currency);
    }
}
