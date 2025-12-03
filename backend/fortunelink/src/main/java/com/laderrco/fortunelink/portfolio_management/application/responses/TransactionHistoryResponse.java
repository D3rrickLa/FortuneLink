package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;

public record TransactionHistoryResponse(List<Transaction> transactions, int totalCount, int pageNumber, int pageSize, Instant dateRangeStart, Instant dateRangeEnd) {
}

