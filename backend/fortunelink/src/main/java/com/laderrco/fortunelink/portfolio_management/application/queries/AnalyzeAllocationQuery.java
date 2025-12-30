package com.laderrco.fortunelink.portfolio_management.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.application.models.AllocationType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

// new item -> BY_TYPE, BY_ACCOUNT, BY_CURRENCY
public record AnalyzeAllocationQuery(UserId userId, AllocationType allocationType, Instant asOfDate) implements ClassValidation {
    public AnalyzeAllocationQuery {
        ClassValidation.validateParameter(userId);
        ClassValidation.validateParameter(allocationType);
    }
}
