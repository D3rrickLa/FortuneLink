package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Quantity Value Object Unit Tests")
class QuantityTest {
  @Nested
  @DisplayName("Creation and Scaling")
  class CreationTests {
    @Test
    @DisplayName("constructor: success applies bankers rounding")
    void constructorAppliesBankersRounding() {
      // HALF_EVEN rounds to the nearest even neighbor for .5 cases
      Quantity q1 = new Quantity(new BigDecimal("2.225"));
      Quantity q2 = new Quantity(new BigDecimal("2.235"));

      assertThat(q1.amount()).isEqualByComparingTo("2.22500000");
      assertThat(q2.amount()).isEqualByComparingTo("2.23500000");
      assertThat(q1.isPositive()).isTrue();
    }

    @Test
    @DisplayName("constructor: fail on null amount")
    void constructorThrowsOnNullAmount() {
      assertThatThrownBy(() -> new Quantity(null))
          .isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("constructor: fail when quantity is negative")
    void constructorThrowsOnNegativeValue() {
      assertThatThrownBy(() -> new Quantity(BigDecimal.valueOf(-10)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Quantity cannot be negative");
    }

    @Test
    @DisplayName("ZERO: success constant check")
    void zeroConstantIsCorrect() {
      assertThat(Quantity.ZERO.amount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(Quantity.ZERO.isZero()).isTrue();
    }
  }

  @Nested
  @DisplayName("Arithmetic Operations")
  class ArithmeticTests {
    @Test
    @DisplayName("add: success simple addition")
    void addPerformsSimpleAddition() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      Quantity q2 = new Quantity(new BigDecimal("5.50"));
      assertThat(q1.add(q2).amount()).isEqualByComparingTo("15.50");
    }

    @Test
    @DisplayName("subtract: success valid result")
    void subtractReturnsValidResult() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      Quantity q2 = new Quantity(new BigDecimal("4.00"));
      assertThat(q1.subtract(q2).amount()).isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("subtract: fail when result is negative")
    void subtractThrowsWhenResultIsNegative() {
      Quantity q1 = new Quantity(new BigDecimal("5.00"));
      Quantity q2 = new Quantity(new BigDecimal("10.00"));

      assertThatThrownBy(() -> q1.subtract(q2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("multiply: success with positive factor")
    void multiplyWorksWithPositiveFactor() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      Quantity result = q1.multiply(new BigDecimal("2.5"));
      assertThat(result.amount()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("multiply: failure with negative factor")
    void multiplyFailsWithnegativeFactor() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      assertThatThrownBy(() -> q1.multiply(new BigDecimal("-1.0"))).isInstanceOf(
          IllegalArgumentException.class);
    }

    @Test
    @DisplayName("divide: success handles repeating decimals")
    void divideHandlesRepeatingDecimalsWithoutException() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      assertThatCode(() -> q1.divide(new BigDecimal("3"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("divide: fail when divisor is zero or negative")
    void divideThrowsOnInvalidDivisor() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));

      assertThatThrownBy(() -> q1.divide(new BigDecimal("-3")))
          .isInstanceOf(IllegalArgumentException.class);

      assertThatThrownBy(() -> q1.divide(new BigDecimal("0")))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Predicates and Comparisons")
  class PredicateTests {
    @Test
    @DisplayName("predicates: success check state")
    void predicatesReturnCorrectBooleans() {
      Quantity pos = new Quantity(new BigDecimal("1.00"));
      Quantity zero = Quantity.ZERO;

      assertThat(pos.isPositive()).isTrue();
      assertThat(pos.isNonZero()).isTrue();
      assertThat(zero.isZero()).isTrue();
      assertThat(zero.isPositive()).isFalse();
    }

    @Test
    @DisplayName("compareTo: success ordering")
    void compareToOrdersCorrectly() {
      Quantity small = new Quantity(new BigDecimal("1.00"));
      Quantity large = new Quantity(new BigDecimal("2.00"));

      assertThat(small).isLessThan(large);
      assertThat(large).isGreaterThan(small);
    }
  }
}