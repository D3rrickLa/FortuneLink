package com.laderrco.fortunelink.PortfolioManagement.domain.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

// impelement in infra layer
public interface ExchangeRateService {
    public BigDecimal getCurrencyExchangeRate(Currency fromCurrency, Currency toCurrency, Instant atInstant);
}
