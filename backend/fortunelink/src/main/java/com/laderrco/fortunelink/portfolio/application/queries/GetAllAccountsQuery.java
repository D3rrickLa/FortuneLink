package com.laderrco.fortunelink.portfolio.application.queries;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record GetAllAccountsQuery(PortfolioId portfolioId, UserId userId, int page, int size) {

  public GetAllAccountsQuery {
    if (portfolioId == null)
      throw new IllegalArgumentException("PortfolioId is required");
    if (userId == null)
      throw new IllegalArgumentException("UserId is required");
    if (page < 0)
      throw new IllegalArgumentException("Page cannot be negative");
    if (size <= 0 || size > 50)
      throw new IllegalArgumentException("Page size must be between 1 and 50");
  }

  /**
   * Convenience constructor, keeps existing callers that don't care about
   * pagination compiling.
   */
  public GetAllAccountsQuery(PortfolioId portfolioId, UserId userId) {
    this(portfolioId, userId, 0, 50);
  }

  public Pageable pageable() {
    return PageRequest.of(page, size, Sort.by("createdDate").ascending());
  }
}
