package com.laderrco.fortunelink.portfolio_management.domain.model.enums;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;

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

    /**
     * Calculate cost basis delta for a transaction of this type
     * Default: for trades, it's cashDelta - fees; others may override
     *
     * @param cashDelta
     * @param totalFees
     * @return 
     */
    public Money calculateCostBasisDelta(Money cashDelta, Money totalFees) {
        if (affectsHoldings) {
            return cashDelta.add(totalFees); // Buying adds cost basis, selling reduces cost basis
        } else {
            return Money.ZERO(cashDelta.currency()); // Non-holdings transactions do not affect cost basis
        }
    }
}