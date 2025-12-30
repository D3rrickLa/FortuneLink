package com.laderrco.fortunelink.portfolio_management.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

/* 
    Instant startDate, Instant endDate, TransactionType transactionType, AccountId accountId are optional
*/
public record GetTransactionHistoryQuery(UserId userId, AccountId accountId, Instant startDate, Instant endDate, TransactionType transactionType, int pageNumber, int pageSize) implements ClassValidation {
    public GetTransactionHistoryQuery {
        ClassValidation.validateParameter(userId);
        // accountId can be null (means all accounts)
        // startDate can be null (means from beginning)
        // endDate can be null (means until now)
        // transactionType can be null (means all types)
        
        // Validate pagination
        // Use Spring's built-in validation by attempting to create a PageRequest
        // This automatically handles negative page numbers and non-positive page sizes
        try {
            org.springframework.data.domain.PageRequest.of(pageNumber, pageSize);
        } 
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid pagination parameters: " + e.getMessage());
        }

        if (pageSize > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
    }
    
    // Convenience constructor with defaults
    public GetTransactionHistoryQuery(UserId userId) {
        this(userId, null, null, null, null, 0, 20);
    }

    // update this if you want sorting later
    public org.springframework.data.domain.Pageable toPageable() {
        return org.springframework.data.domain.PageRequest.of(pageNumber, pageSize);
    }
    
}
