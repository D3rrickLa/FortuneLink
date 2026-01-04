package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.queries;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;

public record TransactionQuery(UUID portfolioId, UUID accountId, TransactionType transactionType,
        LocalDateTime startDate, LocalDateTime endDate, Set<String> assetSymbols) {

    public TransactionQuery {
        // Use Objects.requireNonNullElse for the set to avoid null checks later
        assetSymbols = assetSymbols != null ? assetSymbols : Set.of();

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

    }

    // Helper methods to keep the implementation layer clean
    public Instant startInstant() {
        return startDate != null ? startDate.toInstant(ZoneOffset.UTC) : null;
    }

    public Instant endInstant() {
        return endDate != null ? endDate.toInstant(ZoneOffset.UTC) : null;
    }

}
