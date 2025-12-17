package com.laderrco.fortunelink.portfolio_management.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

// account id optional
public record ViewPerformanceQuery(UserId userId, Instant startDate, Instant endDate, AccountId accountId) implements ClassValidation {
    public ViewPerformanceQuery {
        ClassValidation.validateParameter(userId);
        ClassValidation.validateParameter(startDate);
        ClassValidation.validateParameter(endDate);
        ClassValidation.validateParameter(accountId);
    }
}