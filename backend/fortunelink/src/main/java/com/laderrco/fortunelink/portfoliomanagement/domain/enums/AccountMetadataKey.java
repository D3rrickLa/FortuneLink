package com.laderrco.fortunelink.portfoliomanagement.domain.enums;

public enum AccountMetadataKey {
    WITHHOLDING_TAX_RATE("withholding_tax_rate"),
    TAX_YEAR("tax_year"),
    
    GROSS_DIVIDEND("gross_dividend"),
    GROSS_DIVIDEND_CURRENCY("gross_dividend_currency"),
    WITHHOLDING_TAX_AMOUNT("withholding_tax_amount"),
    EXCHANGE_RATE("exchange_rate"),
    SOURCE_COUNTRY("source_country"),
    INVESTOR_RESIDENCY("investor_residency"),
    TREATY_APPLIED("treaty_applied"),
    FOREIGN_TAX_CREDIT_ELIGIBLE("foreign_tax_credit_eligible"),
    WITHHOLDING_UNKNOWN("withholding_unknown"), // Flag for later adjustment
    RELATED_TRANSACTION_ID("related_transaction_id"),
    TAX_TYPE("tax_type");
    
    private final String key;
    
    AccountMetadataKey(String key) {
        this.key = key;
    }
    
    public String getKey() {
        return key;
    }
    
    @Override
    public String toString() {
        return key;
    }    
}
