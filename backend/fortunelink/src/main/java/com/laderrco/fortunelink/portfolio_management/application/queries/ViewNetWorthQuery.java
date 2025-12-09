package com.laderrco.fortunelink.portfolio_management.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

// asOfDate Optional
public record ViewNetWorthQuery(UserId userId, Instant asOfDate) {
    
}
