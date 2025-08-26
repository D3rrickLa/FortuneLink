package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

// optional classication of inflow for reporting
public enum IncomeType implements TransactionType {
    INTEREST_INCOME,
    STAKING_REWARD,
    BONUS,
    RENTAL_INCOME,
    GRANT;

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.INCOME;
    }    
}
