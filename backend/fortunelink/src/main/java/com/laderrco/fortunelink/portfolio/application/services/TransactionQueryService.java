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
     * Ownership verified via portfolioId + userId + accountId.
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
     */
    public Page<TransactionView> getTransactionHistory(GetTransactionHistoryQuery query) {
        Objects.requireNonNull(query, "GetTransactionHistoryQuery cannot be null");
        validateOwnership(query.portfolioId(), query.userId());
        validatePagination(query.page(), query.size());
        validateDateRange(query.startDate(), query.endDate());

        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by("transactionDate").descending());

        // Route to the right repository method based on what filters are provided
        Page<Transaction> page;

        if (query.startDate() != null && query.endDate() != null) {
            page = transactionQueryRepository.findByAccountIdAndDateRange(
                    query.accountId(), query.startDate(), query.endDate(), pageable);
        } else if (query.symbol() != null) {
            page = transactionQueryRepository.findByAccountIdAndSymbol(
                    query.accountId(), query.symbol(), pageable);
        } else {
            page = transactionQueryRepository.findByAccountId(query.accountId(), pageable);
        }

        return page.map(transactionViewMapper::toTransactionView);
    }

    /**
     * Unbounded fetch for internal calculations (performance, tax reports).
     * Date range is MANDATORY - this is not a user-facing list endpoint.
     *
     * Never call this from a controller directly.
     */
    public List<Transaction> getTransactionsForCalculation(AccountId accountId, Instant start, Instant end) {
        Objects.requireNonNull(accountId, "AccountId cannot be null");
        Objects.requireNonNull(start, "Start date cannot be null for calculation queries");
        Objects.requireNonNull(end, "End date cannot be null for calculation queries");
        validateDateRange(start, end);

        return transactionRepository.findByDateRange(accountId, start, end);
    }

    private void validateOwnership(PortfolioId portfolioId, UserId userId) {
        portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));
    }

    private void validateDateRange(Instant start, Instant end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidDateRangeException(
                    String.format("Start date cannot be after end date: start=%s, end=%s", start, end));
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0)
            throw new IllegalArgumentException("Page cannot be negative: " + page);
        if (size <= 0)
            throw new IllegalArgumentException("Page size must be positive: " + size);
        if (size > 100)
            throw new IllegalArgumentException("Page size cannot exceed 100: " + size);
    }

}
