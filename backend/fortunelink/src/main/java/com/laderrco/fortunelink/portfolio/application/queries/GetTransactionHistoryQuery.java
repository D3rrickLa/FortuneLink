package com.laderrco.fortunelink.portfolio.application.queries;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

/* 
    Instant startDate, Instant endDate, TransactionType transactionType, AccountId accountId are optional
    NOTE: removed transactionType as it was dead code for now, will add back later
*/
public record GetTransactionHistoryQuery(PortfolioId portfolioId, UserId userId, AccountId accountId,
        AssetSymbol symbol, Instant startDate, Instant endDate, int page, int size) {
    public GetTransactionHistoryQuery {
        // accountId can be null (means all accounts)
        // startDate can be null (means from beginning)
        // endDate can be null (means until now)
        // transactionType can be null (means all types)

        validatePagination(page, size);
    }

    // Convenience constructor with defaults
    public GetTransactionHistoryQuery(PortfolioId portfolioId, UserId userId, AssetSymbol symbol) {
        this(portfolioId, userId, null, symbol, null, null, 0, 20);
    }

    // update this if you want sorting later
    public Pageable toPageable() {
        return PageRequest.of(page, size);
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
