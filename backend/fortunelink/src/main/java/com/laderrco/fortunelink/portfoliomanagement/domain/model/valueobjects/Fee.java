package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.time.Instant;
import java.util.Map;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.FeeType;

public record Fee(
    FeeType feetype, 
    Money amount, 
    Money convertedAmount, 
    ExchangeRate exchangeRate, 
    Map<String, String> metadata,
    Instant conversionDate 

) {
    
}
