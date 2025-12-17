package com.laderrco.fortunelink.portfolio_management.application.queries;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record GetPortfolioSummaryQuery(UserId userId) implements ClassValidation{
    public GetPortfolioSummaryQuery {
        ClassValidation.validateParameter(userId);
    }
}
