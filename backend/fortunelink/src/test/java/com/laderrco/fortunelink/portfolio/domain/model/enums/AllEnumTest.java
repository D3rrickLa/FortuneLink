package com.laderrco.fortunelink.portfolio.domain.model.enums;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.laderrco.fortunelink.shared.enums.Precision;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Enum Attribute and Behavioral Validation")
class AllEnumTest {

  @Test
  @DisplayName("assetType: verifies precision, category, and trading flags")
  void assetTypeAttributesAndBehavior() {
    AssetType bond = AssetType.BOND;
    assertEquals(Precision.BOND, bond.precision());
    assertEquals(AssetCategory.FIXED_INCOME, bond.category());
    assertTrue(bond.isMarketTraded());
    assertFalse(bond.isCrypto());

    assertTrue(AssetType.CRYPTO.isCrypto());
    assertFalse(AssetType.REAL_ESTATE.isMarketTraded());
  }

  @Test
  @DisplayName("feeType: verifies tax status, cost basis impact, and deductibility")
  void feeTypeCategoryAndTaxImpact() {
    FeeType brokerage = FeeType.BROKERAGE;
    assertEquals(FeeCategory.TRADING, brokerage.category());
    assertFalse(brokerage.isTax());
    assertTrue(brokerage.affectsCostBasis());
    assertFalse(brokerage.isDeductibleExpense());

    assertTrue(FeeType.ADVISORY_FEE.isDeductibleExpense());
    assertTrue(FeeType.STAMP_DUTY.isTax());
    assertFalse(FeeType.STAMP_DUTY.affectsCostBasis());
  }

  @Test
  @DisplayName("positionStrategy: verifies display names and regulatory descriptions")
  void positionStrategyDisplayMetadata() {
    PositionStrategy acb = PositionStrategy.ACB;
    assertEquals("Average Cost Basis", acb.getDisplayName());
    assertEquals("Required for Canadian tax reporting", acb.getDescription());
  }

  @Test
  @DisplayName("transactionType: verifies holding impact and taxability flags")
  void transactionTypeImpactAndTaxability() {
    TransactionType buy = TransactionType.BUY;
    assertTrue(buy.affectsHoldings());
    assertTrue(buy.isTaxable());

    TransactionType deposit = TransactionType.DEPOSIT;
    assertFalse(deposit.affectsHoldings());
    assertFalse(deposit.isTaxable());
  }
}