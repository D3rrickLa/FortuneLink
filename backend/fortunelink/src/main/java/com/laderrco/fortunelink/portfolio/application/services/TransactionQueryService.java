package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidDateRangeException;
import com.laderrco.fortunelink.portfolio.application.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionForCalculationQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryService {
  private final TransactionRepository transactionRepository;
  private final TransactionQueryRepository transactionQueryRepository;
  private final TransactionViewMapper transactionViewMapper;
  private final PortfolioLoader portfolioLoader;

  public TransactionView getTransaction(GetTransactionByIdQuery query) {
    Objects.requireNonNull(query, "GetTransactionByIdQuery cannot be null");

    Transaction transaction = transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(
            query.transactionId(), query.portfolioId(), query.userId(), query.accountId())
        .orElseThrow(() -> new TransactionNotFoundException(query.transactionId()));

    return transactionViewMapper.toTransactionView(transaction);
  }

  public Page<TransactionView> getTransactionHistory(GetTransactionHistoryQuery query) {
    Objects.requireNonNull(query, "GetTransactionHistoryQuery cannot be null");

    // 1. Security & Basic Validation
    portfolioLoader.validatePortfolioAndAccountOwnership(query.portfolioId(), query.userId(),
        query.accountId());

    validateDateRange(query.startDate(), query.endDate());

    // 2. Execute the single, unified dynamic query
    // This replaces all the 'if (hasDateRange) ... else if (hasSymbol)' logic
    Page<Transaction> page = transactionQueryRepository.findTransactionsDynamic(query.accountId(),
        query.symbol(), query.startDate(), query.endDate(), query.toPageable());

    return page.map(transactionViewMapper::toTransactionView);
  }

  /**
   * Unbounded fetch for internal calculations only. NEVER expose this through a controller
   * endpoint.
   * <p>
   * Used in PerformanceCalculationService - needs full transaction history to calc time-weighted
   * returns. Additionally, used to calculate capital gains, and for planning. Basically, Anything
   * that uses our Transactions (for returns, etc.) we call this.
   */
  public List<Transaction> getTransactionsForCalculation(GetTransactionForCalculationQuery query) {
    Objects.requireNonNull(query, "GetTransactionForCalculationQuery cannot be null");
    Objects.requireNonNull(query.portfolioId(), "PortfolioId cannot be null"); // add
    Objects.requireNonNull(query.userId(), "UserId cannot be null"); // add
    Objects.requireNonNull(query.accountId(), "AccountId cannot be null");
    Objects.requireNonNull(query.start(), "Start date cannot be null for calculation queries");
    Objects.requireNonNull(query.end(), "End date cannot be null for calculation queries");

    portfolioLoader.validatePortfolioAndAccountOwnership(query.portfolioId(), query.userId(),
        query.accountId());
    validateDateRange(query.start(), query.end());

    return transactionRepository.findByAccountIdAndDateRange(query.accountId(), query.start(),
        query.end());
  }

  private void validateDateRange(Instant start, Instant end) {
    if (start != null && end != null && start.isAfter(end)) {
      throw new InvalidDateRangeException(
          String.format("Start date cannot be after end date: start=%s, end=%s", start, end));
    }
  }
}
