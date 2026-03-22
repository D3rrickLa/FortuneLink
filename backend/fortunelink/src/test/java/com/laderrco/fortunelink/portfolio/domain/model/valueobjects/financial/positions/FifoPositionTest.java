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

@DisplayName("FifoPosition Value Object Unit Tests")
class FifoPositionTest {
  private static final AssetSymbol SYMBOL = new AssetSymbol("MSFT");
  private static final AssetType TYPE = AssetType.STOCK;
  private static final Currency USD = Currency.USD;
  private static final Currency CAD = Currency.CAD;
  private static final Instant T1 = Instant.parse("2023-01-01T10:00:00Z");
  private static final Instant T2 = Instant.parse("2023-02-01T10:00:00Z");
  private static final Instant T3 = Instant.parse("2023-03-01T10:00:00Z");

  @Nested
  @DisplayName("Constructor and Validation")
  class ConstructorTests {
    @Test
    @DisplayName("constructor: success with valid initialization")
    void constructor_validInitialization_success() {
      FifoPosition pos = createPosition(createLot("10", "100", T1));
      assertThat(pos.lots()).hasSize(1);
      assertThat(pos).isEqualTo(pos.copy());
    }

    @Test
    @DisplayName("constructor: handles null lots by initializing empty list")
    void constructor_nullLots_initializesEmpty() {
      FifoPosition pos = new FifoPosition(SYMBOL, TYPE, CAD, null, T1);
      assertThat(pos.lots()).isEmpty();
    }

    @Test
    @DisplayName("constructor: handles empty list input")
    void constructor_emptyList_initializesEmpty() {
      FifoPosition pos = new FifoPosition(SYMBOL, TYPE, CAD, List.of(), T1);
      assertThat(pos.lots()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Trading Operations")
  class TradingTests {
    @Test
    @DisplayName("buy: appends new tax lot to position")
    void buy_validLot_appendsToLots() {
      var result = FifoPosition.empty(SYMBOL, TYPE, USD)
          .buy(new Quantity(BigDecimal.TEN), Money.of(100, "USD"), T1);
      assertThat(result.newPosition().lots()).hasSize(1);
    }

    @Test
    @DisplayName("sell: consumes lots in FIFO order")
    void sell_multipleLots_consumesInFifoOrder() {
      FifoPosition pos = createPosition(createLot("10", "100", T1), createLot("10", "200", T2));
      // Sell 15: 10 from T1 ($100) + 5 from T2 ($100)
      var result = pos.sell(new Quantity(new BigDecimal("15")), Money.of(450, "USD"), Instant.now());

      assertThat(result.costBasisSold().amount()).isEqualByComparingTo("200");
      assertThat(result.newPosition().lots()).hasSize(1);
      assertThat(result.newPosition().lots().get(0).quantity().amount()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("sell: preserves untouched subsequent lots")
    void sell_partialConsumption_preservesTrailingLots() {
      FifoPosition pos = FifoPosition.empty(SYMBOL, TYPE, USD)
          .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(100, "USD"), T1).newPosition()
          .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(200, "USD"), T2).newPosition()
          .buy(new Quantity(BigDecimal.valueOf(10)), Money.of(300, "USD"), T3).newPosition();

      var result = pos.sell(new Quantity(BigDecimal.valueOf(15)), Money.of(500, "USD"), Instant.now());
      FifoPosition updated = result.newPosition();

      assertEquals(2, updated.lots().size());
      TaxLot preservedLot = updated.lots().get(1); // The original T3 lot
      assertEquals(T3, preservedLot.acquiredDate());
      assertEquals(new BigDecimal("300.0000000000"), preservedLot.costBasis().amount());
    }

    @Test
    @DisplayName("split: adjusts quantity and basis across all lots")
    void split_validRatio_adjustsAllLots() {
      FifoPosition pos = createPosition(createLot("10", "100", T1));
      var result = pos.split(new Ratio(2, 1));
      assertThat(result.newPosition().totalQuantity().amount()).isEqualByComparingTo("20");
      assertThat(result.newPosition().totalCostBasis().amount()).isEqualByComparingTo("100");
    }
  }

  @Nested
  @DisplayName("Return of Capital (ROC) Logic")
  class RocTests {
    @Test
    @DisplayName("applyReturnOfCapital: reduces cost basis proportionally")
    void applyReturnOfCapital_standardCase_reducesProportionally() {
      FifoPosition pos = createPosition(createLot("10", "400", T1), createLot("10", "600", T1));
      Price rocPrice = Price.of(new BigDecimal("5"), USD); // $100 total

      var result = (ApplyResult.Adjustment<FifoPosition>) pos.applyReturnOfCapital(rocPrice, Quantity.of(20));
      FifoPosition updated = (FifoPosition) result.getUpdatedPosition();

      assertThat(updated.lots().get(0).costBasis().amount()).isEqualByComparingTo("360");
      assertThat(updated.lots().get(1).costBasis().amount()).isEqualByComparingTo("540");
    }

    @Test
    @DisplayName("applyReturnOfCapital: handles ROC exceeding total basis as gain")
    void applyReturnOfCapital_excessRoc_returnsCapitalGain() {
      FifoPosition pos = createPosition(createLot("10", "100", T1));
      Price rocPrice = Price.of(new BigDecimal("15"), USD); // $150 total

      var result = (ApplyResult.RocAdjustment<FifoPosition>) pos.applyReturnOfCapital(rocPrice, Quantity.of(10));

      assertThat(result.getUpdatedPosition().totalCostBasis().isZero()).isTrue();
      assertThat(result.excessCapitalGain().amount()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("applyReturnOfCapital: throws when quantity doesn't match position")
    void applyReturnOfCapital_mismatchedQuantity_throwsException() {
      FifoPosition pos = createPosition(createLot("10", "100", T1));
      assertThatThrownBy(() -> pos.applyReturnOfCapital(Price.of(BigDecimal.ONE, USD), Quantity.of(5)))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("applyLotReduction: clamps last lot to zero when reduction exceeds basis")
    void applyLotReduction_lastLotExcess_clampsToZero() {
      Money result = FifoPosition.applyLotReduction(Money.of("0.01", USD), Money.of("0.03", USD), true, USD);
      assertThat(result).isEqualTo(Money.zero(USD));
    }

    @Test
    @DisplayName("applyLotReduction: throws if intermediate lot would go negative")
    void applyLotReduction_intermediateLotExcess_throwsIllegalState() {
      assertThatThrownBy(() -> FifoPosition.applyLotReduction(Money.of("0.01", USD), Money.of("0.03", USD), false, USD))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("Position Aggregations")
  class AggregationTests {
    @Test
    @DisplayName("totalCostBasis: correctly sums all lots")
    void totalCostBasis_multipleLots_returnsSum() {
      FifoPosition pos = createPosition(createLot("10", "100", T1), createLot("10", "200", T2));
      assertThat(pos.totalCostBasis().amount()).isEqualByComparingTo("300");
    }

    @Test
    @DisplayName("costPerUnit: returns zero when position is empty")
    void costPerUnit_emptyPosition_returnsZero() {
      FifoPosition emptyPos = FifoPosition.empty(SYMBOL, TYPE, USD);
      assertThat(emptyPos.costPerUnit().isZero()).isTrue();
    }

    @Test
    @DisplayName("costPerUnit: calculates weighted average price")
    void costPerUnit_populatedPosition_calculatesWeightedAverage() {
      FifoPosition pos = createPosition(createLot("10", "100", T1), createLot("10", "200", T2));
      // $300 / 20 units = $15
      assertThat(pos.costPerUnit().amount()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("currentValue: calculates total market value")
    void currentValue_validPrice_returnsTotalValue() {
      FifoPosition pos = createPosition(createLot("10", "100", T1));
      Money value = pos.currentValue(Price.of(new BigDecimal("15"), USD));
      assertThat(value.amount()).isEqualByComparingTo("150");
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

  // --- Helpers used across all nested classes ---
  private static TaxLot createLot(String qty, String basis, Instant date) {
    return new TaxLot(new Quantity(new BigDecimal(qty)), new Money(new BigDecimal(basis), USD), date);
  }

  private static FifoPosition createPosition(TaxLot... lots) {
    return new FifoPosition(SYMBOL, TYPE, USD, List.of(lots), Instant.now());
  }
}