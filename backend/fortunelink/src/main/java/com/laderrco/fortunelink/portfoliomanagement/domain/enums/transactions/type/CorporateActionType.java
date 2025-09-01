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
    
    DIVIDEND_REVERSAL,
    STOCK_SPLIT_REVERSAL,
    REVERSE_STOCK_SPLIT_REVERSAL,
    MERGER_REVERSAL,
    SPIN_OFF_REVERSAL,
    RIGHTS_ISSUE_REVERSAL,
    LIQUIDATION_REVERSAL,
    OTHER_REVERSAL;


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

    @Override
    public TransactionType getReversalType() {
        switch (this) {
            case DIVIDEND: return DIVIDEND_REVERSAL;
            case STOCK_SPLIT: return STOCK_SPLIT_REVERSAL;
            case REVERSE_STOCK_SPLIT: return REVERSE_STOCK_SPLIT_REVERSAL;
            case MERGER: return MERGER_REVERSAL;
            case SPIN_OFF: return SPIN_OFF_REVERSAL;
            case RIGHTS_ISSUE: return RIGHTS_ISSUE_REVERSAL;
            case LIQUIDATION: return LIQUIDATION_REVERSAL;
            case OTHER: return OTHER_REVERSAL;
            case DIVIDEND_REVERSAL:
            case STOCK_SPLIT_REVERSAL:
            case REVERSE_STOCK_SPLIT_REVERSAL:
            case MERGER_REVERSAL:
            case SPIN_OFF_REVERSAL:
            case RIGHTS_ISSUE_REVERSAL:
            case LIQUIDATION_REVERSAL:
            case OTHER_REVERSAL:
                throw new UnsupportedOperationException("Reversal transactions cannot be reversed.");

            default:
                throw new IllegalStateException("Unknown transaction type: " + this);
        }
    }
}
