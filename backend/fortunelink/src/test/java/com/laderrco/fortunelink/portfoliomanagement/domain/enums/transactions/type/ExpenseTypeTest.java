package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

public class ExpenseTypeTest {

    @Test
    void testGetCodeReturnsEnumName() {
        assertEquals("FEE", ExpenseType.FEE.getCode());
        assertEquals("EXPENSE_REVERSAL", ExpenseType.EXPENSE_REVERSAL.getCode());
    }

    @Test
    void testGetCategoryAlwaysExpense() {
        for (ExpenseType type : ExpenseType.values()) {
            assertEquals(TransactionCategory.EXPENSE, type.getCategory());
        }
    }

    @Test
    void testIsReversal() {
        assertFalse(ExpenseType.TAX.isReversal());
        assertTrue(ExpenseType.TAX_REVERSAL.isReversal());
    }

    @Test
    void testGetReversalTypeForNonReversal() {
        assertEquals(ExpenseType.FEE_REVERSAL, ExpenseType.FEE.getReversalType());
        assertEquals(ExpenseType.TAX_REVERSAL, ExpenseType.TAX.getReversalType());
        assertEquals(ExpenseType.INTEREST_EXPENSE_REVERSAL, ExpenseType.INTEREST_EXPENSE.getReversalType());
        assertEquals(ExpenseType.MARGIN_INTEREST_REVERSAL, ExpenseType.MARGIN_INTEREST.getReversalType());
        assertEquals(ExpenseType.EXPENSE_REVERSAL, ExpenseType.EXPENSE.getReversalType());
        assertEquals(ExpenseType.OTHER_REVERSAL, ExpenseType.OTHER.getReversalType());
    }

    @Test
    void testGetReversalTypeThrowsForReversalTypes() {
        assertThrows(UnsupportedOperationException.class, () -> ExpenseType.FEE_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> ExpenseType.TAX_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class,
                () -> ExpenseType.INTEREST_EXPENSE_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> ExpenseType.MARGIN_INTEREST_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> ExpenseType.EXPENSE_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> ExpenseType.OTHER_REVERSAL.getReversalType());
    }
}
