package com.laderrco.fortunelink.portfolio_management.application.queries.views;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record PortfolioView(PortfolioId portfolioId, UserId userId, String name, String description, List<AccountView> accounts, Money totalValue, long transactionCount, Instant createDate, Instant lastUpdated) implements ClassValidation {
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