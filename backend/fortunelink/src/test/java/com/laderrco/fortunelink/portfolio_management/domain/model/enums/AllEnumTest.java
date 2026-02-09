package com.laderrco.fortunelink.portfolio_management.domain.model.enums;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;

public class AllEnumTest {
    @Test
    void testAssetType_All() {
        AssetType type = AssetType.BOND;
        assertEquals(Precision.BOND, type.precision());
        assertEquals(AssetCategory.FIXED_INCOME, type.category());
        assertTrue(type.isMarketTraded());
        assertFalse(type.isCrypto());
        assertTrue(AssetType.CRYPTO.isCrypto());
        assertFalse(AssetType.REAL_ESTATE.isMarketTraded());
    }

    @Test 
    void testFeeType_All() {
        FeeType type = FeeType.BROKERAGE;
        assertEquals(FeeCategory.TRADING, type.category());
        assertFalse(type.isTax());
        assertTrue(type.affectsCostBasis());
        assertFalse(type.isDeductibleExpense());

        assertTrue(FeeType.ADVISORY_FEE.isDeductibleExpense());
        assertTrue(FeeType.STAMP_DUTY.isTax());
        assertFalse(FeeType.STAMP_DUTY.affectsCostBasis());
    }

    @Test
    void testPositionStrategy_All() {
        PositionStrategy type = PositionStrategy.ACB;
        assertEquals("Average Cost Basis", type.getDisplayName());
        assertEquals("Required for Canadian tax reporting", type.getDescription());
    }
    
    @Test
    void testTransactionType_All() {
        TransactionType type = TransactionType.BUY;
        assertTrue(type.affectsHoldings());
        assertTrue(type.isTaxable());
    
        assertFalse(TransactionType.DEPOSIT.affectsHoldings());
        assertFalse(TransactionType.DEPOSIT.isTaxable());

    }

}
