package com.laderrco.fortunelink.portfolio.application.utils.annotations;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;

public interface HasAccountId extends HasPortfolioId {
  AccountId accountId();
}
