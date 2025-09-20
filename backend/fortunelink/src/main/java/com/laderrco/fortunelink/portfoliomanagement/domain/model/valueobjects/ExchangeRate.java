package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRate(BigDecimal rate, Instant exchangeDate) {
    
}
