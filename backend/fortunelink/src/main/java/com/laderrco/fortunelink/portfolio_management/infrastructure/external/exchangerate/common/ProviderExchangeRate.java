package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProviderExchangeRate(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate date, String source) {}