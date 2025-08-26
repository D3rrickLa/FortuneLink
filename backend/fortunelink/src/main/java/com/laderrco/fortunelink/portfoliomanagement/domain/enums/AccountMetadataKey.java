package com.laderrco.fortunelink.portfoliomanagement.domain.enums;

public enum AccountMetadataKey {
    WITHHOLDING_TAX_RATE("withholding_tax_rate"),
    FOREIGN_EXCHANGE_FEE("foreign_exchange_fee"),
    TAX_YEAR("tax_year"),
    DRIP_ELIGIBLE("drip_eligible"),
    BROKERAGE_FEE("brokerage_fee"),
    EXCHANGE_RATE_SOURCE("exchange_rate_source");
    
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
