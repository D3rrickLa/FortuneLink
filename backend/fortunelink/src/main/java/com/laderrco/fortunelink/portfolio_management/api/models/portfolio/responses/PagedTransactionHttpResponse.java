package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses;

import java.util.List;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.dtos.DateRangeResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.dtos.PaginationMeta;

public record PagedTransactionHttpResponse(
        List<TransactionHttpResponse> transactions,
        PaginationMeta meta,
        DateRangeResponse dateRange) {
}