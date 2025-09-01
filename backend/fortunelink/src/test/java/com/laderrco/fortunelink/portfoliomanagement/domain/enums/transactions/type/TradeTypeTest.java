package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

public class TradeTypeTest {
    @Test
    void testGetCodeReturnsEnumName() {
        assertEquals("BUY", TradeType.BUY.getCode());
        assertEquals("SELL_REVERSAL", TradeType.SELL_REVERSAL.getCode());
    }

    @Test
    void testGetCategoryAlwaysTrade() {
        for (TradeType type : TradeType.values()) {
            assertEquals(TransactionCategory.TRADE, type.getCategory());
        }
    }

    @Test
    void testIsReversal() {
        assertFalse(TradeType.SELL.isReversal());
        assertTrue(TradeType.SELL_REVERSAL.isReversal());
    }

    @Test
    void testGetReversalTypeForNonReversal() {
        assertEquals(TradeType.BUY_REVERSAL, TradeType.BUY.getReversalType());
        assertEquals(TradeType.SELL_REVERSAL, TradeType.SELL.getReversalType());
        assertEquals(TradeType.SHORT_SELL_REVERSAL, TradeType.SHORT_SELL.getReversalType());
        assertEquals(TradeType.COVER_SHORT_REVERSAL, TradeType.COVER_SHORT.getReversalType());
        assertEquals(TradeType.OPTIONS_EXERCISED_REVERSAL, TradeType.OPTIONS_EXERCISED.getReversalType());
        assertEquals(TradeType.OPTIONS_ASSIGNED_REVERSAL, TradeType.OPTIONS_ASSIGNED.getReversalType());
        assertEquals(TradeType.OPTIONS_EXPIRED_REVERSAL, TradeType.OPTIONS_EXPIRED.getReversalType());
        assertEquals(TradeType.CRYPTO_SWAP_REVERSAL, TradeType.CRYPTO_SWAP.getReversalType());
    }

    @Test
    void testGetReversalTypeThrowsForReversalTypes() {
        assertThrows(UnsupportedOperationException.class, () -> TradeType.BUY_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> TradeType.SELL_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> TradeType.SHORT_SELL_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> TradeType.COVER_SHORT_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> TradeType.OPTIONS_EXERCISED_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> TradeType.OPTIONS_ASSIGNED_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> TradeType.OPTIONS_EXPIRED_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> TradeType.CRYPTO_SWAP_REVERSAL.getReversalType());
    }
}
