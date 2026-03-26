package com.laderrco.fortunelink.portfolio.application.utils.valueobjects;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import java.util.List;

public record UpdatePortfolioResult(Portfolio portfolio, List<AccountId> accountIds) {

}
