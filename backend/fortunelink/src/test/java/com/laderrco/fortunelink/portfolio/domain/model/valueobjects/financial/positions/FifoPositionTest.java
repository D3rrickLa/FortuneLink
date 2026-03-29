package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

  // --- Helpers used across all nested classes ---
  private static TaxLot createLot(String qty, String basis, Instant date) {
    return new TaxLot(new Quantity(new BigDecimal(qty)), new Money(new BigDecimal(basis), USD),
        date);
  }

  private static FifoPosition createPosition(TaxLot... lots) {
    return new FifoPosition(SYMBOL, TYPE, USD, List.of(lots), Instant.now());
  }

  @Nested
  @DisplayName("Constructor and Validation")
  class CreationTests {
    @Test
    @DisplayName("constructor: success with valid initialization")
    void constructorSuccessfullyValidInitialization() {
      FifoPosition pos = createPosition(createLot("10", "100", T1));
      assertThat(pos.lots()).hasSize(1);
      assertThat(pos).isEqualTo(pos.copy());
    }

    @Test
    @DisplayName("constructor: handles null lots by initializing empty list")
    void constructorInitializesEmptyWithNullLots() {
      FifoPosition pos = new FifoPosition(SYMBOL, TYPE, CAD, null, T1);
      assertThat(pos.lots()).isEmpty();
    }

    @Test
    @DisplayName("constructor: handles empty list input")
    void constructorInitializesEmptyWithEmptyList() {
      FifoPosition pos = new FifoPosition(SYMBOL, TYPE, CAD, List.of(), T1);
      assertThat(pos.lots()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Trading Operations")
  class TradingTests {

    @Test
    @DisplayName("buy: appends new tax lot to position")
    void buyValidLotAppendsToLots() {
      var result = FifoPosition.empty(SYMBOL, TYPE, USD)
          .buy(new Quantity(BigDecimal.TEN), Money.of(100, "USD"), T1);

      assertThat(result.newPosition().lots()).hasSize(1);
    }

    @Test
    @DisplayName("sell: consumes lots in FIFO order")
    void sellMultipleLotsConsumesInFifoOrder() {
      FifoPosition pos = createPosition(createLot("10", "100", T1), createLot("10", "200", T2));

      var result = pos.sell(new Quantity(new BigDecimal("15")), Money.of(450, "USD"),
          Instant.now());

      assertThat(result.costBasisSold().amount()).isEqualByComparingTo("200");
      assertThat(result.newPosition().lots()).hasSize(1);
      assertThat(result.newPosition().lots().getFirst().quantity().amount()).isEqualByComparingTo(
          "5");
    }

    @Test
    @DisplayName("sell: preserves untouched subsequent lots")
    void sellPartialConsumptionPreservesTrailingLots() {
      FifoPosition pos = createPosition(createLot("10", "100", T1), createLot("10", "200", T2),
          createLot("10", "300", T3));

      var result = pos.sell(new Quantity(BigDecimal.valueOf(15)), Money.of(500, "USD"),
          Instant.now());
      FifoPosition updated = result.newPosition();

      assertThat(updated.lots()).hasSize(2);
      TaxLot preservedLot = updated.lots().get(1);
      assertThat(preservedLot.acquiredDate()).isEqualTo(T3);
      assertThat(preservedLot.costBasis().amount()).isEqualByComparingTo("300");
    }

    @Test
    @DisplayName("split: adjusts quantity and basis across all lots")
    void splitValidRatioAdjustsAllLots() {
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
    void applyReturnOfCapitalStandardCaseReducesProportionally() {
      FifoPosition pos = createPosition(createLot("10", "400", T1), createLot("10", "600", T1));
      Price rocPrice = Price.of(new BigDecimal("5"), USD);

      var result = (ApplyResult.Adjustment<FifoPosition>) pos.applyReturnOfCapital(rocPrice,
          Quantity.of(20));
      FifoPosition updated = (FifoPosition) result.getUpdatedPosition();

      assertThat(updated.lots().get(0).costBasis().amount()).isEqualByComparingTo("360");
      assertThat(updated.lots().get(1).costBasis().amount()).isEqualByComparingTo("540");
    }

    @Test
    @DisplayName("applyReturnOfCapital: returns RocAdjustment when totalCostBasis is zero")
    void applyReturnOfCapitalReturnsRocAdjustmentWhenCostBasisIsZero() {
      Quantity heldQuantity = Quantity.of(30);
      FifoPosition pos = new FifoPosition(SYMBOL, TYPE, CAD,
          List.of(new TaxLot(heldQuantity, Money.zero(CAD), T1)), T1);
      Price rocPrice = Price.of("2", CAD);

      var result = pos.applyReturnOfCapital(rocPrice, heldQuantity);
      ApplyResult.RocAdjustment<FifoPosition> rocResult = (ApplyResult.RocAdjustment<FifoPosition>) result;

      assertThat(rocResult.excessCapitalGain()).isEqualTo(Money.of("60", CAD));
      assertThat(rocResult.getUpdatedPosition()).isEqualTo(pos);
    }

    @Test
    @DisplayName("applyReturnOfCapital: handles ROC exceeding total basis as gain")
    void applyReturnOfCapitalExcessRocReturnsCapitalGain() {
      FifoPosition pos = createPosition(createLot("10", "100", T1));
      Price rocPrice = Price.of(new BigDecimal("15"), USD);

      var result = (ApplyResult.RocAdjustment<FifoPosition>) pos.applyReturnOfCapital(rocPrice,
          Quantity.of(10));

      assertThat(result.getUpdatedPosition().totalCostBasis().isZero()).isTrue();
      assertThat(result.excessCapitalGain().amount()).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("applyLotReduction: clamps last lot to zero when reduction exceeds basis")
    void applyLotReductionLastLotExcessClampsToZero() {
      Money result = FifoPosition.applyLotReduction(Money.of("0.01", USD), Money.of("0.03", USD),
          true, USD);
      assertThat(result).isEqualTo(Money.zero(USD));
    }

    @Test
    @DisplayName("applyLotReduction: throws if intermediate lot would go negative")
    void applyLotReductionIntermediateLotExcessThrowsIllegalState() {
      assertThatThrownBy(
          () -> FifoPosition.applyLotReduction(Money.of("0.01", USD), Money.of("0.03", USD), false,
              USD)).isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("Position Aggregations")
  class AggregationTests {

    @Test
    @DisplayName("totalCostBasis: correctly sums all lots")
    void totalCostBasisMultipleLotsReturnsSum() {
      FifoPosition pos = createPosition(createLot("10", "100", T1), createLot("10", "200", T2));
      assertThat(pos.totalCostBasis().amount()).isEqualByComparingTo("300");
    }

    @Test
    @DisplayName("costPerUnit: returns zero when position is empty")
    void costPerUnitEmptyPositionReturnsZero() {
      FifoPosition emptyPos = FifoPosition.empty(SYMBOL, TYPE, USD);
      assertThat(emptyPos.costPerUnit().isZero()).isTrue();
    }

    @Test
    @DisplayName("costPerUnit: calculates weighted average price")
    void costPerUnitPopulatedPositionCalculatesWeightedAverage() {
      FifoPosition pos = createPosition(createLot("10", "100", T1), createLot("10", "200", T2));
      assertThat(pos.costPerUnit().amount()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("currentValue: calculates total market value")
    void currentValueValidPriceReturnsTotalValue() {
      FifoPosition pos = createPosition(createLot("10", "100", T1));
      Money value = pos.currentValue(Price.of(new BigDecimal("15"), USD));
      assertThat(value.amount()).isEqualByComparingTo("150");
    }
  }
}