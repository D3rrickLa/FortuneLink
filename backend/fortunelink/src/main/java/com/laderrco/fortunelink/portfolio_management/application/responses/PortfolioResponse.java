package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record PortfolioResponse(PortfolioId portfolioId, UserId userId, List<AccountResponse> accounts, Money totalValue, long transactionCount, Instant createDate, Instant lastUpdated) implements ClassValidation {
    public PortfolioResponse {
        ClassValidation.validateParameter(portfolioId);
        ClassValidation.validateParameter(userId);
        ClassValidation.validateParameter(accounts);
        ClassValidation.validateParameter(totalValue);
        ClassValidation.validateParameter(transactionCount);
        ClassValidation.validateParameter(createDate);
        ClassValidation.validateParameter(lastUpdated);
    }
}