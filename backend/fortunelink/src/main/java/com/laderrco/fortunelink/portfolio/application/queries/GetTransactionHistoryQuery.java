package com.laderrco.fortunelink.portfolio.application.queries;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/*
 * Instant startDate, Instant endDate, TransactionType transactionType, AccountId accountId are
 * optional NOTE: removed transactionType as it was dead code for now, will add back later
 */
public record GetTransactionHistoryQuery(
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    AssetSymbol symbol,
    Instant startDate,
    Instant endDate,
    int page,
    int size) {
  public GetTransactionHistoryQuery {
    // startDate can be null (means from beginning)
    // endDate can be null (means until now)
    // transactionType can be null (means all types)

    // NOTE: originally we did not have this, as in 'null' it means all accounts.
    // that is kind of hard to do so for now, MVP, requires an accountId
    if (accountId == null) {
      throw new IllegalArgumentException("AccountId is required for transaction history queries");
    }

    validatePagination(page, size);
  }

  public Pageable toPageable() {
    return PageRequest.of(page, size, Sort.by("occurredAt").descending());
  }

    private void validatePagination(int page, int size) {
    if (page < 0) {
      throw new IllegalArgumentException("Page cannot be negative: " + page);
    }

    if (size <= 0) {
      throw new IllegalArgumentException("Page size must be positive: " + size);
    }

    if (size > 100) {
      throw new IllegalArgumentException("Page size cannot exceed 100: " + size);
    }
  }

}
