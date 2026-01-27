package com.laderrco.fortunelink.portfolio_management.application.queries.views;

import java.util.List;

import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record TransactionHistoryView(
        List<TransactionView> transactions,
        int totalElements,
        int pageNumber,
        int pageSize,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        DateRangeView dateRange) implements ClassValidation {
    public TransactionHistoryView {
        ClassValidation.validateParameter(transactions);
        ClassValidation.validateParameter(totalElements);
        ClassValidation.validateParameter(pageNumber);
        ClassValidation.validateParameter(pageSize);
        ClassValidation.validateParameter(dateRange);
    }
}
