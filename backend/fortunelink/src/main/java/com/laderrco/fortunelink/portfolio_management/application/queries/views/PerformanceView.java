package com.laderrco.fortunelink.portfolio_management.application.queries.views;

import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public record PerformanceView(Percentage totalReturns, Percentage annualizedReturn, Money realizedGains, Money unrealizedGains, Percentage timeWeightedReturn, Money moneyWeightedReturn, String period) implements ClassValidation {
    public PerformanceView {
        ClassValidation.validateParameter(totalReturns);
        ClassValidation.validateParameter(annualizedReturn);
        ClassValidation.validateParameter(realizedGains);
        ClassValidation.validateParameter(unrealizedGains);
        ClassValidation.validateParameter(timeWeightedReturn);
        // ClassValidation.validateParameter(moneyWeightedReturn);
        ClassValidation.validateParameter(period);
    }
}