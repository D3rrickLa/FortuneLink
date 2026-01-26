package com.laderrco.fortunelink.portfolio_management.application.queries;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record GetAccountSummaryQuery(PortfolioId portfolioId, AccountId accountId) implements ClassValidation {
    public GetAccountSummaryQuery {
        ClassValidation.validateParameter(portfolioId);
        ClassValidation.validateParameter(accountId);
    }
    
}
