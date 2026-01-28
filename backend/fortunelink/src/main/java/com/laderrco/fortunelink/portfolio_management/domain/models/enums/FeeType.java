package com.laderrco.fortunelink.portfolio_management.domain.models.enums;

public enum FeeType {
        // --- Trading & Investment Fees ---
    BROKERAGE(FeeCategory.TRADING),
    TRANSACTION_FEE(FeeCategory.TRADING),
    COMMISSION(FeeCategory.TRADING),
    SPREAD(FeeCategory.TRADING),
    EXCHANGE_FEE(FeeCategory.TRADING),
    CLEARING_FEE(FeeCategory.TRADING),
    MARGIN_INTEREST(FeeCategory.TRADING),
    SHORT_BORROW_FEE(FeeCategory.TRADING),
    PERFORMANCE_FEE(FeeCategory.TRADING),
    MANAGEMENT_FEE(FeeCategory.TRADING),
    CUSTODY_FEE(FeeCategory.TRADING),
    FRONT_END_LOAD(FeeCategory.TRADING),
    BACK_END_LOAD(FeeCategory.TRADING),
    REDEMPTION_FEE(FeeCategory.TRADING),

    // --- Banking & Payment Fees ---
    ACCOUNT_MAINTENANCE(FeeCategory.BANKING),
    WITHDRAWAL_FEE(FeeCategory.BANKING),
    DEPOSIT_FEE(FeeCategory.BANKING),
    WIRE_TRANSFER_FEE(FeeCategory.BANKING),
    OVERDRAFT_FEE(FeeCategory.BANKING),
    LATE_PAYMENT_FEE(FeeCategory.BANKING),
    NSF_FEE(FeeCategory.BANKING),               // Non-Sufficient Funds / Returned Payment
    CHARGEBACK_FEE(FeeCategory.BANKING),
    INTERCHANGE_FEE(FeeCategory.BANKING),
    DORMANCY_FEE(FeeCategory.BANKING),
    PAPER_STATEMENT_FEE(FeeCategory.BANKING),

    // --- Lending & Credit Fees ---
    ORIGINATION_FEE(FeeCategory.LENDING),
    PREPAYMENT_PENALTY(FeeCategory.LENDING),
    ANNUAL_FEE(FeeCategory.LENDING),
    SERVICING_FEE(FeeCategory.LENDING),

    // --- Regulatory & Tax ---
    REGULATORY(FeeCategory.REGULATORY),
    STAMP_DUTY(FeeCategory.REGULATORY),
    TRANSACTION_TAX(FeeCategory.REGULATORY),
    WITHHOLDING_TAX(FeeCategory.REGULATORY),

    // --- Currency & Conversion ---
    FOREIGN_EXCHANGE_CONVERSION(FeeCategory.CURRENCY),
    CONVERSION_SPREAD(FeeCategory.CURRENCY),

    // --- Miscellaneous / Other ---
    GAS(FeeCategory.MISCELLANEOUS),
    PROCESSING(FeeCategory.MISCELLANEOUS),
    ADVISORY_FEE(FeeCategory.MISCELLANEOUS),
    PLATFORM_FEE(FeeCategory.MISCELLANEOUS),
    INSURANCE_FEE(FeeCategory.MISCELLANEOUS),
    OTHER(FeeCategory.MISCELLANEOUS),
    UNKNOWN(FeeCategory.MISCELLANEOUS);

    private final FeeCategory category;

    FeeType(FeeCategory category) {
        this.category = category;
    }

    public FeeCategory getCategory() {
        return category;
    }
}
