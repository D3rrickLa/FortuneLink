package com.laderrco.fortunelink.portfolio_management.application.queries;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

public record GetAccountSummaryQuery(UserId userId, AccountId accountId) {
    
}
