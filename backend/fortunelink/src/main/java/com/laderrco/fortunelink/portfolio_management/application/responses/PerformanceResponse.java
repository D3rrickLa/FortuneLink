package com.laderrco.fortunelink.portfolio_management.application.responses;

import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public record PerformanceResponse(Percentage totalReturns, Percentage annualizedReturn, Money realizedGains, Money unrealizedGains, Percentage timeWeightedReturn, Money moneyWeightedReturn, String period) implements ClassValidation {
    public PerformanceResponse {
        ClassValidation.validateParameter(totalReturns);
        ClassValidation.validateParameter(annualizedReturn);
        ClassValidation.validateParameter(realizedGains);
        ClassValidation.validateParameter(unrealizedGains);
        ClassValidation.validateParameter(timeWeightedReturn);
        // ClassValidation.validateParameter(moneyWeightedReturn);
        ClassValidation.validateParameter(period);
    }
}