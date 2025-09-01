package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

// optional classication of inflow for reporting
public enum IncomeType implements TransactionType {
    INTEREST_INCOME,
    STAKING_REWARD,
    BONUS,
    RENTAL_INCOME,
    GRANT,

    INTEREST_INCOME_REVERSAL,
    STAKING_REWARD_REVERSAL,
    BONUS_REVERSAL,
    RENTAL_INCOME_REVERSAL,
    GRANT_REVERSAL;


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

    @Override
    public TransactionType getReversalType() {
        switch (this) {
            case INTEREST_INCOME: return INTEREST_INCOME_REVERSAL;
            case STAKING_REWARD: return STAKING_REWARD_REVERSAL;
            case BONUS: return BONUS_REVERSAL;
            case RENTAL_INCOME: return RENTAL_INCOME_REVERSAL;
            case GRANT: return GRANT_REVERSAL;
            case INTEREST_INCOME_REVERSAL:
            case STAKING_REWARD_REVERSAL:
            case BONUS_REVERSAL:
            case RENTAL_INCOME_REVERSAL:
            case GRANT_REVERSAL:
                throw new UnsupportedOperationException("Reversal transaction cannot be reversed.");
            default:
                throw new IllegalStateException("Unknown transaction type: " + this);
        }
    }    
}
