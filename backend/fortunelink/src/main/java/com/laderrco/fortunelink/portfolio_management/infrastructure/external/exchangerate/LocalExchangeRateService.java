package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyAreTheSameException;
import com.laderrco.fortunelink.shared.valueobjects.Money;

@Service
public class LocalExchangeRateService implements ExchangeRateService {

    @Override
    public BigDecimal getExchangeRate(ValidatedCurrency from, ValidatedCurrency to) throws CurrencyAreTheSameException {
        return BigDecimal.ONE;
    }

    @Override
    public Money convert(Money amount, ValidatedCurrency targetCurrency, Instant asOfDate) {
        return amount;
    }

    @Override
    public Money convert(Money amount, ValidatedCurrency targetCurrency) {
        return amount;
    }
    
}
