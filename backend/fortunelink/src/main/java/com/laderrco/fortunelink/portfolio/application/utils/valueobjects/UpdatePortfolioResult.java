package com.laderrco.fortunelink.portfolio.application.utils.valueobjects;

import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;

public record UpdatePortfolioResult(Portfolio portfolio, List<AccountId> accountIds) {
  
}
