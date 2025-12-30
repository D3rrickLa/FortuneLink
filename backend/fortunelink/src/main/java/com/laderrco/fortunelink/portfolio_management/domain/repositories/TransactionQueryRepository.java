package com.laderrco.fortunelink.portfolio_management.domain.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;

// pageable -> Spring data interface handling pagination and sortinf for DB queries
// setting it to null = load all at once
// Read-only queries only - NO save() method    
public interface TransactionQueryRepository {
    List<Transaction> findByPortfolioId(PortfolioId portfolioId, Pageable pageable);
    Page<Transaction> findByAccountId(AccountId accountId, Pageable pageable);
    List<Transaction> findByDateRange(PortfolioId portfolioId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    long countByPortfolioId(PortfolioId portfolioId);
    long countByAccountId(AccountId accountId);

    // Add filtering support
    Page<Transaction> findByPortfolioIdAndTransactionType(PortfolioId portfolioId, TransactionType transactionType, Pageable pageable);
    Page<Transaction> findByFilters(
        PortfolioId portfolioId,
        TransactionType transactionType, // nullable
        LocalDateTime startDate,        // nullable
        LocalDateTime endDate,          // nullable
        Set<String> assetSymbols,      // nullable - for account filtering
        Pageable pageable
    );
    
    /**
     * Finds transactions for a portfolio with optional filters and pagination.
     * 
     * @param portfolioId the portfolio ID
     * @param transactionType optional transaction type filter
     * @param startDate optional start date filter (inclusive)
     * @param endDate optional end date filter (inclusive)
     * @param pageable pagination and sorting information
     * @return a page of filtered transactions
     */
    Page<Transaction> findByPortfolioIdAndFilters(
        PortfolioId portfolioId,
        TransactionType transactionType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Finds transactions for a specific account with optional filters and pagination.
     * This method leverages the direct relationship between Transaction and Account.
     * 
     * @param accountId the account ID
     * @param transactionType optional transaction type filter
     * @param startDate optional start date filter (inclusive)
     * @param endDate optional end date filter (inclusive)
     * @param pageable pagination and sorting information
     * @return a page of filtered transactions
     */
    Page<Transaction> findByAccountIdAndFilters(
        AccountId accountId,
        TransactionType transactionType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );
}
