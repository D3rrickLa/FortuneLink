package com.laderrco.fortunelink.portfolio.domain.model.enums;

import static com.laderrco.fortunelink.portfolio.domain.model.enums.FeeCategory.*;

public enum FeeType {

    // Trading & Investment
    BROKERAGE(TRADING),
    COMMISSION(TRADING),
    SPREAD(TRADING),
    EXCHANGE_FEE(TRADING),
    CLEARING_FEE(TRADING),
    PERFORMANCE_FEE(TRADING),
    MANAGEMENT_FEE(TRADING),
    CUSTODY_FEE(TRADING),

    // Banking
    ACCOUNT_MAINTENANCE(BANKING),
    WITHDRAWAL_FEE(BANKING),
    WIRE_TRANSFER_FEE(BANKING),
    NSF_FEE(BANKING),

    // Lending
    MARGIN_INTEREST(LENDING),
    ORIGINATION_FEE(LENDING),
    PREPAYMENT_PENALTY(LENDING),

    // Taxes
    STAMP_DUTY(TAX),
    TRANSACTION_TAX(TAX),
    WITHHOLDING_TAX(TAX),

    // Currency
    FOREIGN_EXCHANGE_CONVERSION(CURRENCY),
    CONVERSION_SPREAD(CURRENCY),

    // Misc
    GAS(MISCELLANEOUS),
    ADVISORY_FEE(MISCELLANEOUS),
    PLATFORM_FEE(MISCELLANEOUS),
    OTHER(MISCELLANEOUS), 
    NONE(MISCELLANEOUS);

    private final FeeCategory category;

    FeeType(FeeCategory category) {
        this.category = category;
    }

    public FeeCategory category() {
        return category;
    }

    public boolean isTax() {
        return category == FeeCategory.TAX;
    }

    public boolean affectsCostBasis() {
        return switch (category) {
            case TRADING, CURRENCY -> true;
            default -> false;
        };
    }

    public boolean isDeductibleExpense() {
        return switch (this) {
            case MANAGEMENT_FEE, CUSTODY_FEE, ADVISORY_FEE -> true;
            default -> false;
        };
    }
}