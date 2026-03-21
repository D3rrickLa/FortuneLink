package com.laderrco.fortunelink.portfolio.domain.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;

public class AccountNotFoundException extends RuntimeException {
  public AccountNotFoundException(String message) {
    super(message);
  }

  public AccountNotFoundException(AccountId accountId, PortfolioId portfolioId) {
    this(String.format("Portfolio, %s does not have/cannot find %s", portfolioId.id(),
        accountId.id()));
  }

}
