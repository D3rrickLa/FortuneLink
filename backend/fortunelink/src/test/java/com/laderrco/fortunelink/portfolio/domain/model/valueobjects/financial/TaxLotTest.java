package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TaxLot Value Object Unit Tests")
class TaxLotTest {
  private final Instant acquiredDate = Instant.parse("2023-01-01T10:00:00Z");
  private final Quantity tenShares = new Quantity(new BigDecimal("10.00"));
  private final Money thousandUsd = Money.of(1000.00, "USD");

  @Nested
  @DisplayName("Creation and Validation")
  class CreationTests {
    @Test
    @DisplayName("constructor: success on valid initialization")
    void constructorInitializesCorrectly() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);
      assertThat(lot.quantity()).isEqualTo(tenShares);
      assertThat(lot.costBasis()).isEqualTo(thousandUsd);
    }

    @Test
    @DisplayName("constructor: fail on negative cost basis")
    void constructorThrowsOnNegativeCostBasis() {
      Money negativeMoney = Money.of(-100.00, "USD");
      assertThatThrownBy(() -> new TaxLot(tenShares, negativeMoney, acquiredDate)).isInstanceOf(
          IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Proportional Logic")
  class ProportionalTests {
    @Test
    @DisplayName("proportionalCost: success on partial sale math")
    void proportionalCostCalculatesCorrectlyForPartialSale() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);
      Quantity fourShares = new Quantity(new BigDecimal("4.00"));

      // 4/10 = 40% of $1000 = $400
      Money result = lot.proportionalCost(fourShares);
      assertThat(result.amount()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("proportionalCost: success with zero quantity")
    void proportionalCostReturnsZeroForZeroQuantity() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);
      assertThat(lot.proportionalCost(Quantity.ZERO).isZero()).isTrue();
    }

    @Test
    @DisplayName("proportionalCost: fail when sale exceeds lot size")
    void proportionalCostThrowsWhenQuantityExceedsLot() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);
      Quantity elevenShares = new Quantity(new BigDecimal("11.00"));

      assertThatThrownBy(() -> lot.proportionalCost(elevenShares)).isInstanceOf(
          IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Lot Evolution")
  class EvolutionTests {
    @Test
    @DisplayName("remainingAfter: success reduces state correctly")
    void remainingAfterReducesStateCorrectly() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);
      Quantity fourShares = new Quantity(new BigDecimal("4.00"));

      TaxLot remaining = lot.remainingAfter(fourShares);

      assertThat(remaining.quantity().amount()).isEqualByComparingTo("6.00");
      assertThat(remaining.costBasis().amount()).isEqualByComparingTo("600.00");
      assertThat(remaining.acquiredDate()).isEqualTo(acquiredDate);
    }

    @Test
    @DisplayName("remainingAfter: returns same lot if quantity is zero")
    void remainingAfterReturnsOriginalIfQuantityIsZero() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);
      Quantity zeroShares = new Quantity(new BigDecimal("0.00"));

      TaxLot result = lot.remainingAfter(zeroShares);
      assertThat(result).isEqualTo(lot);
    }

    @Test
    @DisplayName("remainingAfter: throws exception as we have more shares then owned")
    void remainingAfterSoldSharsGreaterThanQuantityHeld() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);
      Quantity fourShares = new Quantity(new BigDecimal("1000000.00"));

      assertThrows(IllegalArgumentException.class, () -> lot.remainingAfter(fourShares));
    }

    @Test
    @DisplayName("split: success adjusts quantity only")
    void splitAdjustsQuantityButPreservesCostBasis() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);
      Ratio ratio = new Ratio(2, 1);
      TaxLot splitLot = lot.split(ratio);

      assertThat(splitLot.quantity().amount()).isEqualByComparingTo("20.00");
      assertThat(splitLot.costBasis()).isEqualTo(thousandUsd);
    }

    @Test
    @DisplayName("split: round trip (split and reverse) is lossless")
    void splitAndReverseSplitIsLossless() {
      Quantity initialQty = new Quantity(new BigDecimal("10.00000000"));
      Money initialCost = new Money(new BigDecimal("1000.00"), Currency.of("USD"));
      TaxLot originalLot = new TaxLot(initialQty, initialCost, Instant.now());

      Ratio forwardSplit = new Ratio(3, 1);
      Ratio reverseSplit = new Ratio(1, 3);

      TaxLot reversedLot = originalLot.split(forwardSplit).split(reverseSplit);

      assertEquals(originalLot.quantity(), reversedLot.quantity());
      assertEquals(originalLot, reversedLot);
    }
  }

  @Nested
  @DisplayName("Tax Calculations")
  class TaxTimingTests {
    @Test
    @DisplayName("isLongTerm: success after one year")
    void isLongTermReturnsTrueAfterOneYear() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);

      Instant exactlyOneYear = acquiredDate.plus(365, ChronoUnit.DAYS);
      Instant oneDayShort = acquiredDate.plus(364, ChronoUnit.DAYS);

      assertThat(lot.isLongTerm(exactlyOneYear)).isTrue();
      assertThat(lot.isLongTerm(oneDayShort)).isFalse();
    }

    @Test
    @DisplayName("getHoldingPeriodDays: successful calculating the range")
    void getHoldingPeriodDaysCalculatesRange() {
      TaxLot lot = new TaxLot(tenShares, thousandUsd, acquiredDate);
      Instant tenDaysLater = acquiredDate.plus(10, ChronoUnit.DAYS);

      assertThat(lot.getHoldingPeriodDays(tenDaysLater)).isEqualTo(10);
    }
  }
}