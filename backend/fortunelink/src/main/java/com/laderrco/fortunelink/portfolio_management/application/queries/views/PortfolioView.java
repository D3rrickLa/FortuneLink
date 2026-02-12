package com.laderrco.fortunelink.portfolio_management.application.queries.views;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

import lombok.Builder;

@Builder
public record PortfolioView(PortfolioId portfolioId, UserId userId, String name, String description,
        List<AccountView> accounts, Money totalValue, long transactionCount, Instant createDate, Instant lastUpdated)
        implements ClassValidation {
    public PortfolioView {
        ClassValidation.validateParameter(portfolioId);
        ClassValidation.validateParameter(userId);
        ClassValidation.validateParameter(name);
        ClassValidation.validateParameter(description);
        ClassValidation.validateParameter(accounts);
        ClassValidation.validateParameter(totalValue);
        ClassValidation.validateParameter(transactionCount);
        ClassValidation.validateParameter(createDate);
        ClassValidation.validateParameter(lastUpdated);
    }
}