package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.util.List;

public record TransactionHistoryResponse(List<TransactionResponse> transactions, int totalCount, int pageNumber, int pageSize, String dateRange) {
}

