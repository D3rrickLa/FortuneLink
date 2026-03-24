package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AcbPosition Value Object Unit Tests")
class AcbPositionTest {
  private static final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private static final AssetType TYPE = AssetType.STOCK;
  private static final Currency USD = Currency.USD;
  private static final Instant NOW = Instant.now();

  private static AcbPosition createAcbPosition(String qty, String totalBasis) {
    return new AcbPosition(SYMBOL, TYPE, USD, new Quantity(new BigDecimal(qty)),
        new Money(new BigDecimal(totalBasis), USD), NOW, NOW);
  }

  @Nested
  @DisplayName("buy() Operations")
  class BuyTests {
    @Test
    @DisplayName("buy: initial purchase correctly sets quantity and basis")
    void buyInitialPurchaseCreatesNewState() {
      AcbPosition initial = AcbPosition.empty(SYMBOL, TYPE, USD);
      Quantity buyQty = new Quantity(new BigDecimal("10"));
      Money buyCost = new Money(new BigDecimal("1000.00"), USD);

      var result = initial.buy(buyQty, buyCost, NOW);
      AcbPosition updated = result.newPosition();

      assertEquals(new BigDecimal("10.00000000"), updated.totalQuantity().amount());
      assertEquals(new BigDecimal("1000.0000000000"), updated.totalCostBasis().amount());
    }

    @Test
    @DisplayName("buy: additional buy, uses firstAcquiredAt instead")
    void buyAdditionalPurchaseNewUpdatedPositionReturned() {
      AcbPosition initial = AcbPosition.empty(SYMBOL, TYPE, USD);
      Quantity buyQty = new Quantity(new BigDecimal("10"));
      Money buyCost = new Money(new BigDecimal("1000.00"), USD);

      var result = initial.buy(buyQty, buyCost, NOW);
      AcbPosition updated = result.newPosition();

      assertEquals(new BigDecimal("10.00000000"), updated.totalQuantity().amount());
      assertEquals(new BigDecimal("1000.0000000000"), updated.totalCostBasis().amount());

      Quantity anotherBuyQty = Quantity.of(30);
      Money buyCost2 = new Money(BigDecimal.valueOf(3000), USD);

      Instant newTime = Instant.now().plus(Duration.ofDays(2));
      var results2 = updated.buy(anotherBuyQty, buyCost2, newTime);
      AcbPosition updated2 = results2.newPosition();

      assertThat(updated2.totalQuantity()).isEqualTo(Quantity.of(40));
      assertThat(updated2.totalCostBasis()).isEqualTo(Money.of("4000", USD));
      assertThat(updated2.firstAcquiredAt()).isNotEqualTo(newTime);
    }
  }

  @Nested
  @DisplayName("sell() Operations")
  class SellTests {
    @Test
    @DisplayName("sell: reduces quantity and calculates proportional gain")
    void sellPartialSaleCalculatesRealizedGain() {
      // 10 units at $10/unit
      AcbPosition position = createAcbPosition("10", "100.00");
      Quantity sellQty = new Quantity(new BigDecimal("5"));
      Money proceeds = new Money(new BigDecimal("80.00"), USD);

      var result = position.sell(sellQty, proceeds, NOW);
      AcbPosition updated = result.newPosition();

      assertThat(updated.totalQuantity().amount()).isEqualByComparingTo("5");
      assertThat(updated.totalCostBasis().amount()).isEqualByComparingTo("50");
      assertThat(result.realizedGainLoss().amount()).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("sell: wipes basis to exactly zero on full liquidation")
    void sellFullLiquidationClearsBasisExactly() {
      // Setup position with "difficult" division (3 units for $100)
      AcbPosition position = createAcbPosition("3", "100");

      var result = position.sell(Quantity.of(3), Money.of("150", USD), NOW);
      AcbPosition updated = result.newPosition();

      assertThat(result.costBasisSold()).isEqualTo(Money.of("100", USD));
      assertThat(updated.totalCostBasis().isZero()).isTrue();
      assertThat(updated.totalQuantity().isZero()).isTrue();
    }

    @Test
    @DisplayName("sell: throws exception when selling more than held")
    void sellInsufficientQuantityThrowsException() {
      AcbPosition position = createAcbPosition("10", "100.00");
      assertThrows(IllegalArgumentException.class,
          () -> position.sell(Quantity.of(11), Money.of("150", USD), NOW));
    }
  }

  @Nested
  @DisplayName("split() Operations")
  class SplitTests {
    @Test
    @DisplayName("split: adjusts quantity while keeping total basis fixed")
    void splitValidRatioIncreasesQuantityMaintainsBasis() {
      AcbPosition position = createAcbPosition("10", "100.00");
      var result = position.split(new Ratio(2, 1));
      AcbPosition updated = result.newPosition();

      assertThat(updated.totalQuantity().amount()).isEqualByComparingTo("20");
      assertThat(updated.totalCostBasis().amount()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("split: throws exception on invalid ratios")
    void splitInvalidRatioThrowsException() {
      AcbPosition position = AcbPosition.empty(SYMBOL, TYPE, USD);
      assertThrows(IllegalArgumentException.class, () -> position.split(new Ratio(-1, 1)));
    }
  }

  @Nested
  @DisplayName("Return of Capital (ROC)")
  class RocTests {
    @Test
    @DisplayName("applyReturnOfCapital: reduces total cost basis correctly")
    void applyReturnOfCapitalStandardCaseReducesBasis() {
      AcbPosition position = createAcbPosition("100", "1000");
      Price rocPrice = Price.of(BigDecimal.TWO, USD); // $200 total reduction

      var result = (ApplyResult.Adjustment<AcbPosition>) position.applyReturnOfCapital(rocPrice,
          Quantity.of(100));
      AcbPosition updated = (AcbPosition) result.getUpdatedPosition();

      assertThat(updated.totalCostBasis()).isEqualTo(Money.of("800", USD));
    }

    @Test
    @DisplayName("applyReturnOfCapital: caps basis at zero and generates gain if ROC exceeds basis")
    void applyReturnOfCapitalExcessRocGeneratesCapitalGain() {
      AcbPosition position = createAcbPosition("10", "100");
      Price rocPrice = Price.of(BigDecimal.valueOf(15), USD); // $150 reduction

      var result = (ApplyResult.RocAdjustment<AcbPosition>) position.applyReturnOfCapital(rocPrice,
          Quantity.of(10));

      assertThat(result.getUpdatedPosition().totalCostBasis().isZero()).isTrue();
      assertThat(result.excessCapitalGain()).isEqualTo(Money.of("50", USD));
    }

    @Test
    @DisplayName("applyReturnOfCapital: throws if quantity does not match position")
    void applyReturnOfCapitalMismatchedQuantityThrowsException() {
      AcbPosition position = createAcbPosition("50", "500");
      assertThatThrownBy(() -> position.applyReturnOfCapital(Price.of(BigDecimal.ONE, USD),
          Quantity.of(49))).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Aggregations")
  class CalculationTests {

    @Test
    @DisplayName("costPerUnit: returns weighted average for populated positions")
    void costPerUnitPopulatedPositionReturnsCorrectAverage() {
      AcbPosition position = createAcbPosition("4", "100.00");
      assertThat(position.costPerUnit().amount()).isEqualByComparingTo("25");
    }

    @Test
    @DisplayName("costPerUnit: returns zero for empty positions")
    void costPerUnitEmptyPositionReturnsZero() {
      AcbPosition empty = AcbPosition.empty(SYMBOL, TYPE, USD);
      assertThat(empty.costPerUnit().isZero()).isTrue();
    }

    @Test
    @DisplayName("calculateUnrealizedGain: returns difference between value and basis")
    void calculateUnrealizedGainValidPriceReturnsDifference() {
      AcbPosition position = createAcbPosition("10", "100.00");
      Price marketPrice = Price.of(new BigDecimal("15.00"), USD); // Total market value $150

      Money unrealized = position.calculateUnrealizedGain(marketPrice);
      assertThat(unrealized.amount()).isEqualByComparingTo("50");
    }
  }
}