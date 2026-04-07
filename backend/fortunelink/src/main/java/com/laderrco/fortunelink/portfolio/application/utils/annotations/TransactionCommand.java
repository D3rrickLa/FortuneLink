package com.laderrco.fortunelink.portfolio.application.utils.annotations;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.UUID;

public interface TransactionCommand {
  PortfolioId portfolioId();

  UserId userId();

  AccountId accountId();

  UUID idempotencyKey();
}