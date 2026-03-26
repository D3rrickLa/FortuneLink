package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.HasUserId;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

// we are getting the currency form the web - locale by default, make an account for more fine tuning
public record CreatePortfolioCommand(
    UserId userId,
    String name,
    String description,
    Currency currency,
    boolean createDefaultAccount,
    AccountType defaultAccountType,
    PositionStrategy defaultStrategy) implements HasUserId {
  public CreatePortfolioCommand {
    if (createDefaultAccount && defaultAccountType == null) {
      throw new IllegalArgumentException(
          "Default account type required when createDefaultAccount is true");
    }
  }
}
