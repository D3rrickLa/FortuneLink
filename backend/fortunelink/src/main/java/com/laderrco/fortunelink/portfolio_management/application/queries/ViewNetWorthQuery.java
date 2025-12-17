package com.laderrco.fortunelink.portfolio_management.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

// asOfDate Optional
public record ViewNetWorthQuery(UserId userId, Instant asOfDate) implements ClassValidation {
    public ViewNetWorthQuery {
        ClassValidation.validateParameter(userId);
        ClassValidation.validateParameter(asOfDate);
    }
}
