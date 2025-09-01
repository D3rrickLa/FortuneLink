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

    FEE_REVERSAL,
    TAX_REVERSAL,
    INTEREST_EXPENSE_REVERSAL,
    MARGIN_INTEREST_REVERSAL,
    EXPENSE_REVERSAL,
    OTHER_REVERSAL;

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

    @Override
    public TransactionType getReversalType() {
        switch (this) {
            case FEE: return FEE_REVERSAL;
            case TAX: return TAX_REVERSAL;
            case INTEREST_EXPENSE: return INTEREST_EXPENSE_REVERSAL;
            case MARGIN_INTEREST: return MARGIN_INTEREST_REVERSAL;
            case EXPENSE: return EXPENSE_REVERSAL;
            case OTHER: return OTHER_REVERSAL;
            case FEE_REVERSAL:
            case TAX_REVERSAL:
            case INTEREST_EXPENSE_REVERSAL:
            case MARGIN_INTEREST_REVERSAL:
            case EXPENSE_REVERSAL:
            case OTHER_REVERSAL:
                throw new UnsupportedOperationException("Reversal transactions cannot be reversed.");

            default:
                throw new IllegalStateException("Unknown transaction type: " + this);
        }
    }
}
