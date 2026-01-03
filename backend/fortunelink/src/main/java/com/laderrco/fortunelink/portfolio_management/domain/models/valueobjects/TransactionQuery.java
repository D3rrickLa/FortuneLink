package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;

public record TransactionQuery(UUID portfolioId, UUID accountId, TransactionType transactionType,
        LocalDateTime startDate, LocalDateTime endDate, Set<String> assetSymbols) {

}
