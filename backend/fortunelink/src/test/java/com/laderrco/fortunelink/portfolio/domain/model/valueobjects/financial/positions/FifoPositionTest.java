package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TaxLot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FifoPositionTest {
  private final AssetSymbol SYMBOL = new AssetSymbol("MSFT");
  private final AssetType TYPE = AssetType.STOCK;
  private final Currency USD = Currency.USD;
  private final Currency CAD = Currency.CAD;
  private final Instant T1 = Instant.parse("2023-01-01T10:00:00Z");
  private final Instant T2 = Instant.parse("2023-02-01T10:00:00Z");
  private final Instant T3 = Instant.parse("2023-03-01T10:00:00Z");

  @Test
  void testConstructorAndCopy() {
    FifoPosition pos = position(lot("10", "100", T1));
    assertThat(pos.lots()).hasSize(1);
    assertThat(pos).isEqualTo(pos.copy());
  }

  @Test
  void testConstructor_sucess_lots_given_null() {
    FifoPosition pos = new FifoPosition(SYMBOL, TYPE, CAD, null, T1);
    assertThat(pos.lots()).hasSize(0);
    assertThat(pos).isEqualTo(pos.copy());
  }

  @Test
  void testConstructor_sucess_lots_given_empty_list() {
    FifoPosition pos = new FifoPosition(SYMBOL, TYPE, CAD, List.of(), T1);
    assertThat(pos.lots()).hasSize(0);
    assertThat(pos).isEqualTo(pos.copy());
  }

  // --- Helpers used across all nested classes ---
  private TaxLot lot(String qty, String basis, Instant date) {
    return new TaxLot(new Quantity(new BigDecimal(qty)), new Money(new BigDecimal(basis), USD),
        date);
  }

  private FifoPosition position(TaxLot... lots) {
    return new FifoPosition(SYMBOL, TYPE, USD, List.of(lots), Instant.now());
  }

  @Nested
  @DisplayName("Trading Logic (Buy/Sell/Split)")
  class TradingTests {
    @Test
    void buy_appendsNewLot() {
      var result = FifoPosition.empty(SYMBOL, TYPE, USD)
          .buy(new Quantity(BigDecimal.TEN), Money.of(100, "USD"), T1);
      assertThat(result.newPosition().lots()).hasSize(1);
    }

    @Test
    void sell_consumesInFifoOrder() {
      FifoPosition pos = position(lot("10", "100", T1), lot("10", "200", T2));
      // Sell 15: 10 from Lot1 ($100) + 5 from Lot2 ($100) = $200 cost sold
      var result = pos.sell(new Quantity(new BigDecimal("15")), Money.of(450, "USD"),
          Instant.now());

      assertThat(result.costBasisSold().amount()).isEqualByComparingTo("200");
      assertThat(result.newPosition().lots()).hasSize(1);
      assertThat(result.newPosition().lots().get(0).quantity().amount()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("sell_success_preservesSubsequentLots_whenRemainingToSellIsZero")
    void sell_preservesTrailingLots() {
      // Setup: 3 Lots
      // Lot 1: 10 units (to be fully consumed)
      // Lot 2: 10 units (to be partially consumed)
      // Lot 3: 10 units (should be preserved via the isZero() branch)
      FifoPosition pos = FifoPosition.empty(SYMBOL, TYPE, USD)
          .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), T1).newPosition()
          .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(200, "USD"), T2).newPosition()
          .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(300, "USD"), T3).newPosition();

      // Action: Sell 15 units.
      // This consumes Lot 1, 5 units of Lot 2, and should trigger isZero() for Lot 3.
      var result = pos.sell(new Quantity(BigDecimal.valueOf(15)), Money.of(500, "USD"),
          Instant.now());

      FifoPosition updated = result.newPosition();

      // Verification
      assertEquals(2, updated.lots().size(), "Should have partial Lot 2 and untouched Lot 3");

      // Verify Lot 3 was preserved exactly (Date and Amount)
      TaxLot preservedLot = updated.lots().get(1);
      assertEquals(T3, preservedLot.acquiredDate());
      assertEquals(new BigDecimal("10.00000000"), preservedLot.quantity().amount());
      assertEquals(new BigDecimal("300.0000000000"), preservedLot.costBasis().amount());
    }

    @Test
    void split_adjustsAllLots() {
      FifoPosition pos = position(lot("10", "100", T1));
      var result = pos.split(new Ratio(2, 1));
      assertThat(result.newPosition().totalQuantity().amount()).isEqualByComparingTo("20");
      assertThat(result.newPosition().totalCostBasis().amount()).isEqualByComparingTo("100");
    }
  }

  @Nested
  @DisplayName("Return of Capital (ROC) Logic")
  class RocTests {

    @Test
    @DisplayName("Standard Case: Proportional reduction across lots")
    void applyRoc_proportionalReduction() {
      FifoPosition pos = position(lot("10", "400", T1), lot("10", "600", T1));
      Price rocPrice = Price.of(new BigDecimal("5"), USD); // Total reduction $100

      var result = (ApplyResult.Adjustment<FifoPosition>) pos.applyReturnOfCapital(rocPrice,
          Quantity.of(20));
      FifoPosition updated = (FifoPosition) result.getUpdatedPosition();

      // Lot 1 (40% of basis) -> $360 | Lot 2 (60% of basis) -> $540
      assertThat(updated.lots().get(0).costBasis().amount()).isEqualByComparingTo("360");
      assertThat(updated.lots().get(1).costBasis().amount()).isEqualByComparingTo("540");
    }

    @Test
    @DisplayName("Excess Case: ROC exceeds basis or basis is already zero")
    void applyRoc_handlesExcessGain() {
      FifoPosition pos = position(lot("10", "100", T1));
      Price rocPrice = Price.of(new BigDecimal("15"), USD); // $150 total

      var result = (ApplyResult.RocAdjustment<FifoPosition>) pos.applyReturnOfCapital(rocPrice,
          Quantity.of(10));

      assertThat(result.getUpdatedPosition().totalCostBasis().isZero()).isTrue();
      assertThat(result.excessCapitalGain().amount()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("Should treat entire ROC as gain if cost basis is already zero")
    void applyRoc_returnsRocAdjustmentWhenTotalCostBasisIsZero() {
      Currency usd = Currency.USD;
      AssetSymbol symbol = new AssetSymbol("BTC");
      TaxLot lot = new TaxLot(Quantity.of(1), Money.zero(usd), Instant.now());
      FifoPosition position = new FifoPosition(symbol, AssetType.CRYPTO, usd, List.of(lot),
          Instant.now());

      Price rocPrice = Price.of(BigDecimal.valueOf(100), usd);

      ApplyResult<FifoPosition> result = position.applyReturnOfCapital(rocPrice, Quantity.of(1));
      ApplyResult.RocAdjustment<FifoPosition> rocResult = (ApplyResult.RocAdjustment<FifoPosition>) result;

      assertThat(rocResult.excessCapitalGain()).isEqualTo(Money.of("100", usd));
      assertThat(rocResult.getUpdatedPosition().totalCostBasis()).isEqualTo(Money.zero(usd));
    }

    @Test
    void applyLotReduction_lastLotGoesNegative_clampsToZero() {
      Money lotBasis = Money.of("0.01", USD);
      Money lotReduction = Money.of("0.03", USD); // deliberately exceeds basis

      Money result = FifoPosition.applyLotReduction(lotBasis, lotReduction, true, USD);

      assertThat(result).isEqualTo(Money.zero(USD));
    }

    @Test
    void applyLotReduction_intermediateLotGoesNegative_throwsIllegalState() {
      Money lotBasis = Money.of("0.01", USD);
      Money lotReduction = Money.of("0.03", USD);

      assertThatThrownBy(
          () -> FifoPosition.applyLotReduction(lotBasis, lotReduction, false, USD)).isInstanceOf(
          IllegalStateException.class);
    }

    @Test
    @DisplayName("Precision Case: Last lot absorbs drift within allowed money precision")
    void applyRoc_partialReductionRoundingDrift() {
      // Total Basis = $3.00.
      TaxLot l1 = lot("1", "1.00", T1);
      TaxLot l2 = lot("1", "1.00", T1);
      TaxLot l3 = lot("1", "1.00", T1);
      FifoPosition pos = position(l1, l2, l3);

      // This price has 10 decimal places. (Valid per your Money constructor)
      // Total Reduction = 0.0033333333 * 3 = 0.0099999999
      Price rocPrice = Price.of(new BigDecimal("0.0033333333"), USD);

      var result = pos.applyReturnOfCapital(rocPrice, Quantity.of(3));
      FifoPosition updated = (FifoPosition) result.getUpdatedPosition();

      // Verification:
      // Total Basis (3.00) - Total Reduction (0.0099999999) = 2.9900000001
      assertThat(updated.totalCostBasis().amount()).isEqualByComparingTo("2.9900000001");
    }

    @Test
    @DisplayName("Precision Case: Last lot clamps to zero on rounding drift")
    void applyRoc_lastLotClampsOnRoundingDrift() {
      // Total Basis = $3.00.
      // Lot 1 & 2: $1.00 each. Lot 3: $1.00.
      TaxLot l1 = lot("1", "1.00", T1);
      TaxLot l2 = lot("1", "1.00", T1);
      TaxLot l3 = lot("1", "1.00", T1);
      FifoPosition pos = position(l1, l2, l3);

      // We want total reduction to be slightly MORE than total basis to force the cap
      // $1.01 * 3 units = $3.03 total reduction.
      // Logic caps this at $3.00.
      Price rocPrice = Price.of(new BigDecimal("1.01"), USD);

      var result = pos.applyReturnOfCapital(rocPrice, Quantity.of(3));
      FifoPosition updated = (FifoPosition) result.getUpdatedPosition();

      // Why this works:
      // 1. totalReduction is capped at 3.00.
      // 2. Ratio for Lot 1: 1.00 / 3.00 = 0.3333333333 (Rounds DOWN at scale 10)
      // 3. Reduction Lot 1: 3.00 * 0.3333333333 = 0.9999999999
      // 4. Reduction Lot 2: 3.00 * 0.3333333333 = 0.9999999999
      // 5. Remaining for Lot 3: 3.00 - 0.9999999999 - 0.9999999999 = 1.0000000002
      // 6. Lot 3 New Basis: 1.00 - 1.0000000002 = -0.0000000002 -> CLAMP TO zero!

      assertThat(updated.lots().get(2).costBasis().isZero()).isTrue();
      assertThat(updated.totalCostBasis().isZero()).isTrue();
    }

    @Test
    @DisplayName("Error Case: Quantity mismatch")
    void applyRoc_throwsOnMismatchedQuantity() {
      FifoPosition pos = position(lot("10", "100", T1));
      assertThatThrownBy(() -> pos.applyReturnOfCapital(Price.of(BigDecimal.ONE, USD),
          Quantity.of(5))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyLotReduction_lastLotGoesNegative_returnsZero() {
      Money lotBasis = Money.of("10.00", CAD);
      Money lotReduction = Money.of("15.00", CAD); // exceeds basis

      Money result = FifoPosition.applyLotReduction(lotBasis, lotReduction, true, CAD);

      assertThat(result).isEqualTo(Money.zero(CAD));
    }

    @Test
    void applyLotReduction_intermediateLotGoesNegative_throws() {
      Money lotBasis = Money.of("10.00", CAD);
      Money lotReduction = Money.of("15.00", CAD);

      assertThatThrownBy(
          () -> FifoPosition.applyLotReduction(lotBasis, lotReduction, false, CAD)).isInstanceOf(
          IllegalStateException.class).hasMessageContaining("Intermediate lot went negative");
    }

    @Test
    void applyLotReduction_normalCase_subtractsCorrectly() {
      Money lotBasis = Money.of("100.00", CAD);
      Money lotReduction = Money.of("30.00", CAD);

      Money result = FifoPosition.applyLotReduction(lotBasis, lotReduction, false, CAD);

      assertThat(result).isEqualTo(Money.of("70.00", CAD));
    }
  }

  @Nested
  @DisplayName("Aggregations")
  class AggregationTests {
    @Test
    void calculations_totalBasisAndValue() {
      FifoPosition pos = position(lot("10", "100", T1));
      assertThat(pos.totalCostBasis().amount()).isEqualByComparingTo("100");
      assertThat(pos.costPerUnit().amount()).isEqualByComparingTo("10");
      assertThat(
          pos.currentValue(Price.of(new BigDecimal("15"), USD)).amount()).isEqualByComparingTo(
          "150");
    }
  }

  @Nested
  @DisplayName("costPerUnit() Branch Tests")
  class CostPerUnitTests {

    @Test
    @DisplayName("costPerUnit_success_returnsZeroWhenEmpty")
    void costPerUnit_branch_empty() {
      FifoPosition emptyPos = FifoPosition.empty(SYMBOL, TYPE, USD);

      Money result = emptyPos.costPerUnit();

      assertTrue(result.isZero());
      assertEquals(USD, result.currency());
    }

    @Test
    @DisplayName("costPerUnit_success_calculatesWeightedAverageWhenNotEmpty")
    void costPerUnit_branch_notEmpty() {
      // 10 units @ $100 + 10 units @ $200 = 20 units @ $300
      FifoPosition pos = FifoPosition.empty(SYMBOL, TYPE, USD)
          .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), T1).newPosition()
          .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(200, "USD"), T2).newPosition();

      Money result = pos.costPerUnit();

      // $300 / 20 units = $15.00
      assertEquals(new BigDecimal("15.0000000000"), result.amount());
    }
  }

}