package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

public enum ExpenseType implements TransactionType {
    FEE,
    TAX,
    INTEREST_EXPENSE,
    MARGIN_INTEREST,
    EXPENSE,
    OTHER;

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.EXPENSE;
    }
}
