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
    OTHER;

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.CORPORATE_ACTION;
    }  
}
