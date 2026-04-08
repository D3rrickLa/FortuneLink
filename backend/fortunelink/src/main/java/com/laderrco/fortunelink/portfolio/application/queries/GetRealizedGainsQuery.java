package com.laderrco.fortunelink.portfolio.application.queries;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
    AssetSymbol symbol,
    int page,
    int size) {
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

    // Pagination defaults/validation
    if (page < 0) {
      page = 0;
    }
    if (size <= 0) {
      size = 20;
    }
    if (size > 100) {
      size = 100; // Prevent massive memory requests
    }
  }

  public Pageable toPageable() {
    // Sorting by date descending is usually the expected behavior for gains
    return PageRequest.of(page, size, Sort.by("occurredAt").descending());
  }
}