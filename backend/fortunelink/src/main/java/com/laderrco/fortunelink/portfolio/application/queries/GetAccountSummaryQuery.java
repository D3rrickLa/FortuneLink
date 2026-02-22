package com.laderrco.fortunelink.portfolio.application.queries;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record GetAccountSummaryQuery(PortfolioId portfolioId, UserId userId, AccountId accountId){

    
}
