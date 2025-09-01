package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

public enum CashTransactionType implements TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT,
    REFUND,

    DEPOSIT_REVERSAL,
    WITHDRAWAL_REVERSAL,
    TRANSFER_IN_REVERSAL,
    TRANSFER_OUT_REVERSAL,
    REFUND_REVERSAL;

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.CASH;
    }

    @Override
    public boolean isReversal() {
        return name().contains("REVERSAL");
    }

    @Override
    public TransactionType getReversalType() {
        switch (this) {
            case DEPOSIT: return DEPOSIT_REVERSAL;
            case WITHDRAWAL: return WITHDRAWAL_REVERSAL;
            case REFUND: return REFUND_REVERSAL;
            case TRANSFER_IN: return TRANSFER_IN_REVERSAL;
            case TRANSFER_OUT: return TRANSFER_OUT_REVERSAL;
            // Reversal types throw exception
            case DEPOSIT_REVERSAL:
            case WITHDRAWAL_REVERSAL:
            case REFUND_REVERSAL:
            case TRANSFER_IN_REVERSAL:
            case TRANSFER_OUT_REVERSAL:
                throw new UnsupportedOperationException("Reversal transactions cannot be reversed.");
                
            default:
                throw new IllegalStateException("Unknown transaction type: " + this);
        }
    }
}
