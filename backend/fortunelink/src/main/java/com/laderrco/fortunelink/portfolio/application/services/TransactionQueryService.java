package com.laderrco.fortunelink.portfolio.application.services;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidDateRangeException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryService {

    private final TransactionRepository transactionRepository; // for single lookups
    private final TransactionQueryRepository transactionQueryRepository; // for paginated history
    private final PortfolioRepository portfolioRepository;
    private final TransactionViewMapper transactionViewMapper;

    /**
     * Retrieves a single transaction by ID.
     * Ownership is enforced via the compound key lookup — no separate portfolio
     * load needed.
     */
    public TransactionView getTransaction(GetTransactionByIdQuery query) {
        Objects.requireNonNull(query, "GetTransactionByIdQuery cannot be null");

        Transaction transaction = transactionRepository
                .findByIdAndPortfolioIdAndUserIdAndAccountId(
                        query.transactionId(),
                        query.portfolioId(),
                        query.userId(),
                        query.accountId())
                .orElseThrow(() -> new TransactionNotFoundException(query.transactionId()));

        return transactionViewMapper.toTransactionView(transaction);
    }

    /**
     * Paginated transaction history for an account.
     * Default sort: transactionDate DESC.
     *
     * Filter routing — exactly one filter mode is active per request:
     * 1. Date range (startDate + endDate both present): returns transactions in
     * range.
     * 2. Symbol only (symbol present, no dates): returns transactions for that
     * symbol.
     * 3. No filter: returns all transactions for the account, paginated.
     *
     * Fix #5: Combining date range AND symbol was previously silently ignoring the
     * symbol filter. This is now explicitly rejected — callers must choose one
     * filter
     * mode. If you need date-range + symbol filtering later, add a dedicated
     * repository method and a fourth branch here.
     */
    public Page<TransactionView> getTransactionHistory(GetTransactionHistoryQuery query) {
        Objects.requireNonNull(query, "GetTransactionHistoryQuery cannot be null");

        // pagnation check here is done in query record
        validateOwnership(query.portfolioId(), query.userId());
        validateDateRange(query.startDate(), query.endDate());

        boolean hasDateRange = query.startDate() != null && query.endDate() != null;
        boolean hasSymbol = query.symbol() != null;

        // Reject ambiguous combinations — callers must pick one filter mode
        if (hasDateRange && hasSymbol) {
            throw new IllegalArgumentException(
                    "Cannot filter by both date range and symbol simultaneously. " +
                            "Provide either a date range or a symbol, not both.");
        }

        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by("occurredAt").descending());

        Page<Transaction> page;

        // when we add back transactionType to GetTransactionHistoryQuery
        // we will do the check here for the 'page'
        if (hasDateRange) {
            page = transactionQueryRepository
                    .findByAccountIdAndDateRange(query.accountId(), query.startDate(), query.endDate(), pageable);
        } else if (hasSymbol) {
            page = transactionQueryRepository
                    .findByAccountIdAndSymbol(query.accountId(), query.symbol(), pageable);
        } else {
            page = transactionQueryRepository.findByAccountId(query.accountId(), pageable);
        }

        return page.map(transactionViewMapper::toTransactionView);
    }

    /**
     * Unbounded fetch for internal calculations (performance, tax reports).
     * Date range is MANDATORY — never call this from a controller directly.
     */
    public List<Transaction> getTransactionsForCalculation(AccountId accountId, Instant start, Instant end) {
        Objects.requireNonNull(accountId, "AccountId cannot be null");
        Objects.requireNonNull(start, "Start date cannot be null for calculation queries");
        Objects.requireNonNull(end, "End date cannot be null for calculation queries");

        validateDateRange(start, end);

        return transactionRepository.findByDateRange(accountId, start, end);
    }

    /**
     * Lightweight ownership check — does not load the Portfolio aggregate.
     * Use existsByIdAndUserId wherever you only need to verify access without
     * needing to mutate or read the portfolio itself.
     */
    private void validateOwnership(PortfolioId portfolioId, UserId userId) {
        if (!portfolioRepository.existsByIdAndUserId(portfolioId, userId)) {
            throw new PortfolioNotFoundException(portfolioId);
        }
    }

    private void validateDateRange(Instant start, Instant end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidDateRangeException(
                    String.format("Start date cannot be after end date: start=%s, end=%s", start, end));
        }
    }

}
