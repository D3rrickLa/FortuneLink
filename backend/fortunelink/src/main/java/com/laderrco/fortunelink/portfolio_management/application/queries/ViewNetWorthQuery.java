package com.laderrco.fortunelink.portfolio_management.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

// asOfDate Optional
public record ViewNetWorthQuery(PortfolioId portfolioId, Instant asOfDate) implements ClassValidation {
    public ViewNetWorthQuery {
        ClassValidation.validateParameter(portfolioId);
        // ClassValidation.validateParameter(asOfDate);
    }
}
