package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

public enum CorporateActionType implements TransactionType {
    DIVIDEND,
    STOCK_SPLIT,
    REVERSE_STOCK_SPLIT,
    MERGER,
    SPIN_OFF,
    RIGHTS_ISSUE,
    LIQUIDATION,
    OTHER,
    
    OTHER_CORPORATE_ACTION_TYPE_REVERSAL,
    CORPORATE_ACTION_REVERSAL, 
    STOCK_SPLIT_REVERSAL, 
    DIVIDEND_REVERSAL;


    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.CORPORATE_ACTION;
    }  

    @Override
    public boolean isReversal() {
        return name().contains("REVERSAL");
    }
}
