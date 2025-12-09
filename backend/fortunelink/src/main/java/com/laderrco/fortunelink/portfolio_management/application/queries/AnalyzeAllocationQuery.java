package com.laderrco.fortunelink.portfolio_management.application.queries;

import com.laderrco.fortunelink.portfolio_management.application.models.AllocationType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

// new item -> BY_TYPE, BY_ACCOUNT, BY_CURRENCY
public record AnalyzeAllocationQuery(UserId userId, AllocationType alloactionType) {
    
}
