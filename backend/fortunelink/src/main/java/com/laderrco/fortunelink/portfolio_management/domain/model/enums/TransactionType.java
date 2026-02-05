package com.laderrco.fortunelink.portfolio_management.domain.model.enums;

public enum TransactionType {
    BUY(true, true, true),
    SELL(true, true, true),
    DEPOSIT(false, false, false),
    WITHDRAWAL(false, false, false),
    DIVIDEND(false, true, false),
    DIVIDEND_REINVEST(true, true, true),
    INTEREST(false, true, false),
    FEE(false, false, false),
    SPLIT(true, false, true),
    RETURN_OF_CAPITAL(false, false, false),
    TRANSFER_IN(false, false, false),
    TRANSFER_OUT(false, false, false),
    OTHER(false, false, false),
    REINVESTED_CAPITAL_GAIN(false, true, false);

    private final boolean affectsHoldings;
    private final boolean taxable;
    private final boolean requiresExecution;

    TransactionType(boolean affectsHoldings, boolean taxable, boolean requiresExecution) {
        this.affectsHoldings = affectsHoldings;
        this.taxable = taxable;
        this.requiresExecution = requiresExecution;
    }

    public boolean affectsHoldings() {
        return affectsHoldings;
    }

    public boolean isTaxable() {
        return taxable;
    }

    public boolean requiresExecution() {
        return requiresExecution;
    }
}
