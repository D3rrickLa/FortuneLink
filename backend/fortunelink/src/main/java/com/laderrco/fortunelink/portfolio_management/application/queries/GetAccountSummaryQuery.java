package com.laderrco.fortunelink.portfolio_management.application.queries;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record GetAccountSummaryQuery(UserId userId, AccountId accountId) implements ClassValidation {
    public GetAccountSummaryQuery {
        ClassValidation.validateParameter(userId);
        ClassValidation.validateParameter(accountId);
    }
    
}
