package com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;

public class CashTransactionTypeTest {
    @Test
    void getCode_IsValid() {
        CashTransactionType type = CashTransactionType.DEPOSIT;
        String name =  type.getCode();
        assertEquals(CashTransactionType.DEPOSIT.getCode(), name);
        assertNotEquals(CashTransactionType.DEPOSIT_REVERSAL, name);
    }

    @Test
    void getCategory() {
        CashTransactionType type = CashTransactionType.DEPOSIT;
        assertEquals(TransactionCategory.CASH, type.getCategory());
        assertNotEquals(TransactionCategory.CORPORATE_ACTION, type.getCategory());
    }

    @Test
    void isReversal_IsValid() {
        CashTransactionType type = CashTransactionType.DEPOSIT;
        assertFalse(type.isReversal());
        type = CashTransactionType.REFUND_REVERSAL;
        assertTrue(type.isReversal());
    }

   @Test
    void testGetCodeReturnsEnumName() {
        assertEquals("DEPOSIT", CashTransactionType.DEPOSIT.getCode());
        assertEquals("WITHDRAWAL_REVERSAL", CashTransactionType.WITHDRAWAL_REVERSAL.getCode());
    }

    @Test
    void testGetCategoryAlwaysCash() {
        for (CashTransactionType type : CashTransactionType.values()) {
            assertEquals(TransactionCategory.CASH, type.getCategory());
        }
    }

    @Test
    void testIsReversal() {
        assertFalse(CashTransactionType.DEPOSIT.isReversal());
        assertTrue(CashTransactionType.DEPOSIT_REVERSAL.isReversal());
    }

    @Test
    void testGetReversalTypeForNonReversal() {
        assertEquals(CashTransactionType.DEPOSIT_REVERSAL, CashTransactionType.DEPOSIT.getReversalType());
        assertEquals(CashTransactionType.WITHDRAWAL_REVERSAL, CashTransactionType.WITHDRAWAL.getReversalType());
        assertEquals(CashTransactionType.REFUND_REVERSAL, CashTransactionType.REFUND.getReversalType());
        assertEquals(CashTransactionType.TRANSFER_IN_REVERSAL, CashTransactionType.TRANSFER_IN.getReversalType());
        assertEquals(CashTransactionType.TRANSFER_OUT_REVERSAL, CashTransactionType.TRANSFER_OUT.getReversalType());
    }

    @Test
    void testGetReversalTypeThrowsForReversalTypes() {
        assertThrows(UnsupportedOperationException.class, () -> CashTransactionType.DEPOSIT_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CashTransactionType.WITHDRAWAL_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CashTransactionType.REFUND_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CashTransactionType.TRANSFER_IN_REVERSAL.getReversalType());
        assertThrows(UnsupportedOperationException.class, () -> CashTransactionType.TRANSFER_OUT_REVERSAL.getReversalType());
    }
}
