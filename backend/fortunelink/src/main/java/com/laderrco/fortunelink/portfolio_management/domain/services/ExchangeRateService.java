package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.math.BigDecimal;

import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyAreTheSameException;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public interface ExchangeRateService {
    public BigDecimal getExchangeRate(ValidatedCurrency from, ValidatedCurrency to) throws CurrencyAreTheSameException; 
    public Money convert(Money amount, ValidatedCurrency targetCurrency);
}
