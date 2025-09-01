package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncomeTypeTest {

    @Test
    void testGetCodeReturnsEnumName() {
        assertEquals("INTEREST_INCOME", IncomeType.INTEREST_INCOME.getCode());
        assertEquals("BONUS_REVERSAL", IncomeType.BONUS_REVERSAL.getCode());
    }

    @Test
    void testGetCategoryAlwaysIncome() {
        for (IncomeType type : IncomeType.values()) {
            assertEquals(TransactionCategory.INCOME, type.getCategory());
        }
    }

    @Test
    void testIsReversal() {
        assertFalse(IncomeType.INTEREST_INCOME.isReversal());
        assertTrue(IncomeType.INTEREST_INCOME_REVERSAL.isReversal());
    }

    @Test
    void testGetReversalTypeForNonReversal() {
        assertEquals(IncomeType.INTEREST_INCOME_REVERSAL, IncomeType.INTEREST_INCOME.getReversalType());
        assertEquals(IncomeType.STAKING_REWARD_REVERSAL, IncomeType.STAKING_REWARD.getReversalType());
        assertEquals(IncomeType.BONUS_REVERSAL, IncomeType.BONUS.getReversalType());
        assertEquals(IncomeType.RENTAL_INCOME_REVERSAL, IncomeType.RENTAL_INCOME.getReversalType());
        assertEquals(IncomeType.GRANT_REVERSAL, IncomeType.GRANT.getReversalType());
    }

    @Test
    void testGetReversalTypeThrowsForReversalTypes() {
        assertThrows(UnsupportedOperationException.class, () -> IncomeType.INTEREST_INCOME_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> IncomeType.STAKING_REWARD_REVERSAL.getReversalType()); 
        assertThrows(UnsupportedOperationException.class, () -> IncomeType.BONUS_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> IncomeType.RENTAL_INCOME_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> IncomeType.GRANT_REVERSAL.getReversalType());
    }
}