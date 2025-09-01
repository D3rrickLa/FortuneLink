package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

// Trade Transaction
public enum TradeType implements TransactionType {
    BUY,
    SELL,
    SHORT_SELL,
    COVER_SHORT,
    OPTIONS_EXERCISED,
    OPTIONS_ASSIGNED,
    OPTIONS_EXPIRED,
    CRYPTO_SWAP,
    
    BUY_REVERSAL,
    SELL_REVERSAL,
    SHORT_SELL_REVERSAL,
    COVER_SHORT_REVERSAL,
    OPTIONS_EXERCISED_REVERSAL,
    OPTIONS_ASSIGNED_REVERSAL,
    OPTIONS_EXPIRED_REVERSAL,
    CRYPTO_SWAP_REVERSAL;


    @Override
    public String getCode() {
        return name();
    }

    @Override
    public TransactionCategory getCategory() {
        return TransactionCategory.TRADE;
    }

    @Override
    public boolean isReversal() {
        return name().contains("REVERSAL");
    }

    @Override
    public TransactionType getReversalType() {
        switch (this) {
            case BUY: return BUY_REVERSAL;
            case SELL: return SELL_REVERSAL;
            case SHORT_SELL: return SHORT_SELL_REVERSAL;
            case COVER_SHORT: return COVER_SHORT_REVERSAL;
            case OPTIONS_EXERCISED: return OPTIONS_EXERCISED_REVERSAL;
            case OPTIONS_ASSIGNED: return OPTIONS_ASSIGNED_REVERSAL;
            case OPTIONS_EXPIRED: return OPTIONS_EXPIRED_REVERSAL;
            case CRYPTO_SWAP: return CRYPTO_SWAP_REVERSAL;
            case BUY_REVERSAL:
            case SELL_REVERSAL:
            case SHORT_SELL_REVERSAL:
            case COVER_SHORT_REVERSAL:
            case OPTIONS_EXERCISED_REVERSAL:
            case OPTIONS_ASSIGNED_REVERSAL:
            case OPTIONS_EXPIRED_REVERSAL:
            case CRYPTO_SWAP_REVERSAL:
                throw new UnsupportedOperationException("Reversal transactions cannot be reversed");

            default:
                throw new IllegalStateException("Unknown tranction type: " + this);
        }
    }
}
