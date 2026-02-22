package com.laderrco.fortunelink.portfolio.application.queries;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;


/* 
    Instant startDate, Instant endDate, TransactionType transactionType, AccountId accountId are optional
*/
public record GetTransactionHistoryQuery(PortfolioId portfolioId, UserId userId, AccountId accountId, AssetSymbol symbol, Instant startDate, Instant endDate, TransactionType transactionType, int page, int size) {
    public GetTransactionHistoryQuery {
        // accountId can be null (means all accounts)
        // startDate can be null (means from beginning)
        // endDate can be null (means until now)
        // transactionType can be null (means all types)
        
        // Validate pagination
        // Use Spring's built-in validation by attempting to create a PageRequest
        // This automatically handles negative page numbers and non-positive page sizes
        try {
            PageRequest.of(page, size);
        } 
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid pagination parameters: " + e.getMessage());
        }

        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
    }
    
    // Convenience constructor with defaults
    public GetTransactionHistoryQuery(PortfolioId portfolioId, UserId userId, AssetSymbol symbol) {
        this(portfolioId, userId, null, null, null, null, null, 0, 20);
    }

    // update this if you want sorting later
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
    
}
