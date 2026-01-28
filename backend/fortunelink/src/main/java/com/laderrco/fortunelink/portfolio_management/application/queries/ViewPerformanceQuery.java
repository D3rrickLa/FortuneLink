package com.laderrco.fortunelink.portfolio_management.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

// account id optional
public record ViewPerformanceQuery(PortfolioId portfolioId, AccountId accountId, Instant startDate, Instant endDate) implements ClassValidation {
    public ViewPerformanceQuery {
        ClassValidation.validateParameter(portfolioId);
        ClassValidation.validateParameter(startDate);
        ClassValidation.validateParameter(endDate);
        ClassValidation.validateParameter(accountId);
    }
}