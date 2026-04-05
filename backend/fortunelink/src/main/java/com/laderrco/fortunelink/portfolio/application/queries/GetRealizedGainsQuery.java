package com.laderrco.fortunelink.portfolio.application.queries;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

// Integer taxYear, null = all years
// AssetSymbol symbol, null = all symbols
public record GetRealizedGainsQuery(
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    Integer taxYear,
    AssetSymbol symbol) {
  // NOTE: taxYear and symbol can be combined here, unlike the transaction
  // history filter, because realized gains are a smaller dataset and the
  // JpaRealizedGainRepository handles the combined case with a single query.
  public GetRealizedGainsQuery {
    if (portfolioId == null) {
      throw new IllegalArgumentException("PortfolioId required");
    }
    if (userId == null) {
      throw new IllegalArgumentException("UserId required");
    }
    if (accountId == null) {
      throw new IllegalArgumentException("AccountId required");
    }
  }
}