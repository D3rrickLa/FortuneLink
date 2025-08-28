package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

public enum CashTransactionType implements TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT,
    REFUND,

    OTHER_CASH_TYPE_REVERSAL,
    CASH_REVERSAL,
    DEPOSIT_REVERSAL, 
    WITHDRAWAL_REVERSAL;

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
}
