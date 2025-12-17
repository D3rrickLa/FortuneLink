package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.util.List;

import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record TransactionHistoryResponse(List<TransactionResponse> transactions, int totalCount, int pageNumber, int pageSize, String dateRange) implements ClassValidation {
    public TransactionHistoryResponse {
        ClassValidation.validateParameter(transactions);
        ClassValidation.validateParameter(totalCount);
        ClassValidation.validateParameter(pageNumber);
        ClassValidation.validateParameter(pageSize);
        ClassValidation.validateParameter(dateRange);
    }
}

