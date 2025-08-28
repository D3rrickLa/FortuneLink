package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

// optional classication of inflow for reporting
public enum IncomeType implements TransactionType {
    INTEREST_INCOME,
    STAKING_REWARD,
    BONUS,
    RENTAL_INCOME,
    GRANT,

    OTHER_INCOME_TYPE_REVERSAL,
    INCOME_REVERSAL, 
    DIVIDEND_INCOME_REVERSAL, 
    INTEREST_INCOME_REVERSAL;


    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.INCOME;
    }

    @Override
    public boolean isReversal() {
        return name().contains("REVERSAL");
    }    
}
