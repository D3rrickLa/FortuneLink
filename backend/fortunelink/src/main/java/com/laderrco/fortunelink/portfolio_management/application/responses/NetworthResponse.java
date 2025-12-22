package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;

import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record NetWorthResponse(Money totalAssets, Money totalLiabilities, Money netWorth, Instant asOfDate, ValidatedCurrency currency) implements ClassValidation {
    public NetWorthResponse {
        ClassValidation.validateParameter(totalAssets); 
        ClassValidation.validateParameter(totalLiabilities); 
        ClassValidation.validateParameter(netWorth); 
        ClassValidation.validateParameter(asOfDate); 
        ClassValidation.validateParameter(currency); 
    }
}