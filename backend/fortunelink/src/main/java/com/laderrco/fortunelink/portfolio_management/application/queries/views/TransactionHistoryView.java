package com.laderrco.fortunelink.portfolio_management.application.queries.views;

import java.util.List;

import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

public record TransactionHistoryView(List<TransactionView> transactions, int totalCount, int pageNumber, int pageSize, String dateRange) implements ClassValidation {
    public TransactionHistoryView {
        ClassValidation.validateParameter(transactions);
        ClassValidation.validateParameter(totalCount);
        ClassValidation.validateParameter(pageNumber);
        ClassValidation.validateParameter(pageSize);
        ClassValidation.validateParameter(dateRange);
    }
}

