package com.laderrco.fortunelink.portfolio_management.application.queries;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record GetPortfolioSummaryQuery(PortfolioId portfolioId) implements ClassValidation{
    public GetPortfolioSummaryQuery {
        ClassValidation.validateParameter(portfolioId);
    }
}
