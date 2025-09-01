package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CorporateActionTypeTest {

    @Test
    void testGetCodeReturnsEnumName() {
        assertEquals("DIVIDEND", CorporateActionType.DIVIDEND.getCode());
        assertEquals("MERGER_REVERSAL", CorporateActionType.MERGER_REVERSAL.getCode());
    }

    @Test
    void testGetCategoryAlwaysCorporateAction() {
        for (CorporateActionType type : CorporateActionType.values()) {
            assertEquals(TransactionCategory.CORPORATE_ACTION, type.getCategory());
        }
    }

    @Test
    void testIsReversal() {
        assertFalse(CorporateActionType.DIVIDEND.isReversal());
        assertTrue(CorporateActionType.DIVIDEND_REVERSAL.isReversal());
    }

    @Test
    void testGetReversalTypeForNonReversal() {
        assertEquals(CorporateActionType.DIVIDEND_REVERSAL, CorporateActionType.DIVIDEND.getReversalType());
        assertEquals(CorporateActionType.STOCK_SPLIT_REVERSAL, CorporateActionType.STOCK_SPLIT.getReversalType());
        assertEquals(CorporateActionType.REVERSE_STOCK_SPLIT_REVERSAL, CorporateActionType.REVERSE_STOCK_SPLIT.getReversalType());
        assertEquals(CorporateActionType.MERGER_REVERSAL, CorporateActionType.MERGER.getReversalType());
        assertEquals(CorporateActionType.SPIN_OFF_REVERSAL, CorporateActionType.SPIN_OFF.getReversalType());
        assertEquals(CorporateActionType.RIGHTS_ISSUE_REVERSAL, CorporateActionType.RIGHTS_ISSUE.getReversalType());
        assertEquals(CorporateActionType.LIQUIDATION_REVERSAL, CorporateActionType.LIQUIDATION.getReversalType());
        assertEquals(CorporateActionType.OTHER_REVERSAL, CorporateActionType.OTHER.getReversalType());
    }

    @Test
    void testGetReversalTypeThrowsForReversalTypes() {
        assertThrows(UnsupportedOperationException.class, () -> CorporateActionType.DIVIDEND_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CorporateActionType.STOCK_SPLIT_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CorporateActionType.REVERSE_STOCK_SPLIT_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CorporateActionType.MERGER_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CorporateActionType.SPIN_OFF_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CorporateActionType.RIGHTS_ISSUE_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CorporateActionType.LIQUIDATION_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CorporateActionType.OTHER_REVERSAL.getReversalType());
    }
}
