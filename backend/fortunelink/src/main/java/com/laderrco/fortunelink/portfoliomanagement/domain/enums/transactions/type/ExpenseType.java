package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

// optional classication of outflow for reporting
public enum ExpenseType implements TransactionType {
    FEE,
    TAX,
    INTEREST_EXPENSE,
    MARGIN_INTEREST,
    EXPENSE,
    OTHER,

    OTHER_EXPENSE_TYPE_REVERSAL, 
    EXPENSE_REVERSAL, 
    FEE_REVERSAL, 
    COMMISSION_REVERSAL;

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.EXPENSE;
    }
    
    @Override
    public boolean isReversal() {
        return name().contains("REVERSAL");
    }
}
