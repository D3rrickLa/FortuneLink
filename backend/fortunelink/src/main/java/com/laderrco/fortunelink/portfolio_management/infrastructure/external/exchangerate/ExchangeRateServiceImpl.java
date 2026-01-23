package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate;

import java.math.BigDecimal;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateProvider;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ExchangeRateMapper;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyAreTheSameException;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.RequiredArgsConstructor;
// TODO: IMPL
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);
    private final ExchangeRateMapper mapper;
    private final ExchangeRateProvider provider;

    @Override
    public BigDecimal getExchangeRate(ValidatedCurrency from, ValidatedCurrency to) throws CurrencyAreTheSameException {
        if (from.equals(to)) {
            throw new CurrencyAreTheSameException(from.getCode());
        }

        // latest rate
        return provider.getExchangeRate(from, to, null);
    }

    @Override
    public Money convert(Money amount, ValidatedCurrency targetCurrency, Instant asOfDate) {
        ValidatedCurrency sourceCurrency = amount.currency();
        // Same currency → no conversion
        if (sourceCurrency.equals(targetCurrency)) {
            return amount;
        }

        BigDecimal rate = provider.getExchangeRate(sourceCurrency, targetCurrency, asOfDate);
        BigDecimal converted = amount.amount().multiply(rate);

        return new Money(converted, targetCurrency);
    }

    @Override
    public Money convert(Money amount, ValidatedCurrency targetCurrency) {
        return convert(amount, targetCurrency, null);
    }

}
