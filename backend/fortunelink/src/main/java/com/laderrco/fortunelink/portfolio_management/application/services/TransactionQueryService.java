package com.laderrco.fortunelink.portfolio_management.application.services;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidDateRangeException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidSearchCriteriaException;
import com.laderrco.fortunelink.portfolio_management.application.models.TransactionSearchCriteria;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries.TransactionQuery;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories.TransactionQueryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Application service for querying transactions.
 * 
 * Provides unified interface for transaction queries with proper validation,
 * converting between application-layer DTOs and infrastructure queries.
 * 
 * This service is READ-ONLY. For WRITE operations (creating/updating transactions),
 * use PortfolioApplicationService which operates on the Portfolio aggregate root.
 * 
 * NOTE: we are using the transaction query here ONLY, no where else as this is for querying
 */
@Service
@RequiredArgsConstructor
public class TransactionQueryService {
    private final TransactionQueryRepository repository;

    /**
     * Query transactions with pagination and default sorting (by transactionDate DESC).
     * 
     * Use this for user-facing transaction history pages.
     * 
     * @param criteria Search criteria with domain types (PortfolioId, AccountId, etc.)
     * @param page Page number (0-based index)
     * @param size Number of records per page
     * @return Page of transactions matching criteria
     * @throws InvalidSearchCriteriaException if criteria validation fails
     * @throws InvalidDateRangeException if date range is invalid
     * @throws IllegalArgumentException if pagination params are invalid
     */
    public Page<Transaction> queryTransactions(TransactionSearchCriteria criteria, int page,int size) {
        validateCriteria(criteria);
        
        TransactionQuery query = buildQuery(criteria);
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        
        return repository.find(query, pageable);
    }

    /**
     * Query transactions with pagination and custom sorting.
     * 
     * Use this when you need specific sort orders (e.g., by amount, by symbol).
     * 
     * @param criteria Search criteria
     * @param page Page number (0-based)
     * @param size Page size
     * @param sort Custom sort specification
     * @return Page of transactions
     */
    public Page<Transaction> queryTransactions(TransactionSearchCriteria criteria, int page, int size, Sort sort) {
        Objects.requireNonNull(criteria, "TransactionSearchCriteria cannot be null");
        Objects.requireNonNull(sort, "Sort cannot be null");
        
        validateCriteria(criteria);
        validatePagination(page, size);
        
        TransactionQuery query = buildQuery(criteria);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return repository.find(query, pageable);
    }
    
    /**
     * Get all transactions matching criteria without pagination.
     * 
     * Use this for calculations/reports that need the complete dataset,
     * such as performance analysis, tax reports, or portfolio analytics.
     * 
     * WARNING: Be careful with large datasets! Always use date ranges
     * to limit the result set and prevent memory issues.
     * 
     * @param criteria Search criteria (should include date range for large portfolios)
     * @return List of all matching transactions, sorted by date DESC
     */
    public List<Transaction> getAllTransactions(TransactionSearchCriteria criteria) {
        return getAllTransactions(criteria, Sort.by("transactionDate").descending());
    }
    
    /**
     * Get all transactions with custom sorting.
     * 
     * @param criteria Search criteria
     * @param sort Custom sort specification
     * @return List of all matching transactions
     */
    public List<Transaction> getAllTransactions(TransactionSearchCriteria criteria, Sort sort) {
        Objects.requireNonNull(criteria, "TransactionSearchCriteria cannot be null");
        Objects.requireNonNull(sort, "Sort cannot be null");
        
        validateCriteria(criteria);
        
        TransactionQuery query = buildQuery(criteria);
        
        // Use Pageable.unpaged() with custom sort
        Pageable unpaged = PageRequest.of(0, Integer.MAX_VALUE, sort);
        Page<Transaction> page = repository.find(query, unpaged);
        
        return page.getContent();
    }
    
    /**
     * Converts application-layer criteria (with domain value objects)
     * to infrastructure-layer query (with raw UUID types for database).
     * 
     * This is the translation layer between domain concepts and persistence.
     */
    private TransactionQuery buildQuery(TransactionSearchCriteria criteria) {
        return new TransactionQuery(
            criteria.portfolioId() != null ? criteria.portfolioId().portfolioId() : null,
            criteria.accountId() != null ? criteria.accountId().accountId() : null,
            criteria.transactionType(),
            criteria.startDate(),
            criteria.endDate(),
            criteria.assetSymbols()
        );
    }
    
    /**
     * Validates search criteria business rules.
     * 
     * @throws InvalidDateRangeException if start date is after end date
     * @throws InvalidSearchCriteriaException if required identifiers are missing
     */
    private void validateCriteria(TransactionSearchCriteria criteria) {
        // Validate date range makes logical sense
        if (criteria.startDate() != null && criteria.endDate() != null) {
            if (criteria.startDate().isAfter(criteria.endDate())) {
                throw new InvalidDateRangeException(
                    String.format(
                        "Start date cannot be after end date: start=%s, end=%s",
                        criteria.startDate(),
                        criteria.endDate()
                    )
                );
            }
        }
        
        // At least one scope identifier must be provided
        // (prevents accidentally querying ALL transactions across ALL portfolios)
        if (criteria.portfolioId() == null && criteria.accountId() == null) {
            throw new InvalidSearchCriteriaException(
                "Either portfolioId or accountId must be provided to scope the query"
            );
        }
    }
    
    /**
     * Validates pagination parameters.
     * 
     * @throws IllegalArgumentException if pagination params are invalid
     */
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                String.format("Page number cannot be negative: %d", page)
            );
        }
        
        if (size <= 0) {
            throw new IllegalArgumentException(
                String.format("Page size must be positive: %d", size)
            );
        }
        
        // Prevent accidentally large page sizes that could cause memory issues
        if (size > 1000) {
            throw new IllegalArgumentException(
                String.format(
                    "Page size cannot exceed 1000: %d. " +
                    "Use getAllTransactions() for larger datasets with proper date filtering.",
                    size
                )
            );
        }
    }
}
