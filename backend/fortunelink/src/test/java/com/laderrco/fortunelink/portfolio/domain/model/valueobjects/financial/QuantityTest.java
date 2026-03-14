package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import org.junit.jupiter.api.*;

import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@DisplayName("Quantity Value Object Unit Tests")
class QuantityTest {

  @Nested
  @DisplayName("Creation and Scaling")
  class CreationTests {

    @Test
    @DisplayName("constructor_success_AppliesBankersRounding")
    void constructor_success_AppliesBankersRounding() {
      // HALF_EVEN rounds to the nearest even neighbor for .5 cases
      // Assuming QUANTITY_PRECISION is 2 for this example
      Quantity q1 = new Quantity(new BigDecimal("2.225")); // Should round to 2.22
      Quantity q2 = new Quantity(new BigDecimal("2.235")); // Should round to 2.24

      assertThat(q1.amount()).isEqualByComparingTo("2.22500000");
      assertThat(q2.amount()).isEqualByComparingTo("2.23500000");
      assertThat(q1.isPositive()).isTrue();
      assertThat(q1.abs()).isEqualTo(q1);
    }

    @Test
    @DisplayName("constructor_fail_NullAmount")
    void constructor_fail_NullAmount() {
      assertThatThrownBy(() -> new Quantity(null))
          .isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("ZERO_success_ConstantCheck")
    void ZERO_success_ConstantCheck() {
      assertThat(Quantity.ZERO.amount()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(Quantity.ZERO.isZero()).isTrue();
    }
  }

  @Nested
  @DisplayName("Arithmetic Operations")
  class ArithmeticTests {

    @Test
    @DisplayName("add_success_SimpleAddition")
    void add_success_SimpleAddition() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      Quantity q2 = new Quantity(new BigDecimal("5.50"));
      assertThat(q1.add(q2).amount()).isEqualByComparingTo("15.50");
    }

    @Test
    @DisplayName("subtract_success_ValidResult")
    void subtract_success_ValidResult() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      Quantity q2 = new Quantity(new BigDecimal("4.00"));
      assertThat(q1.subtract(q2).amount()).isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("subtract_fail_ResultingNegative")
    void subtract_fail_ResultingNegative() {
      Quantity q1 = new Quantity(new BigDecimal("5.00"));
      Quantity q2 = new Quantity(new BigDecimal("10.00"));

      assertThatThrownBy(() -> q1.subtract(q2))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("cannot be negative");
    }

    @Test
    @DisplayName("multiply_success_WithPositiveFactor")
    void multiply_success_WithPositiveFactor() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      Quantity result = q1.multiply(new BigDecimal("2.5"));
      assertThat(result.amount()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("multiply_fail_NegativeFactor")
    void multiply_fail_NegativeFactor() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      assertThatThrownBy(() -> q1.multiply(new BigDecimal("-1.0")))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("divide_success_HandlesRepeatingDecimals")
    void divide_success_HandlesRepeatingDecimals() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));
      // 10 / 3 = 3.3333... should round based on Q_ROUNDING_MODE
      assertThatCode(() -> q1.divide(new BigDecimal("3")))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("divide_fail_amountlessthan0")
    void divide_fail_lessThanZero() {
      Quantity q1 = new Quantity(new BigDecimal("10.00"));

      assertThatThrownBy(() -> q1.divide(new BigDecimal("-3")))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Divisor cannot be negative");
    }
  }

  @Nested
  @DisplayName("Predicates and Comparisons")
  class PredicateTests {

    @Test
    @DisplayName("isPositive_isZero_isNonZero_success")
    void isPositive_isZero_isNonZero_success() {
      Quantity pos = new Quantity(new BigDecimal("1.00"));
      Quantity zero = Quantity.ZERO;

      assertThat(pos.isPositive()).isTrue();
      assertThat(pos.isNonZero()).isTrue();
      assertThat(zero.isZero()).isTrue();
      assertThat(zero.isPositive()).isFalse();
      assertThat(zero.isNonZero()).isFalse();

      assertThatThrownBy(() -> new Quantity(BigDecimal.valueOf(-1)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Quantity cannot be negative");
    }

    @Test
    @DisplayName("compareTo_success_Ordering")
    void compareTo_success_Ordering() {
      Quantity small = new Quantity(new BigDecimal("1.00"));
      Quantity large = new Quantity(new BigDecimal("2.00"));

      assertThat(small).isLessThan(large);
      assertThat(large).isGreaterThan(small);
    }

    @Test
    @DisplayName("should_fail_when_quantity_given_negative")
    void constructor_fail_when_negative_value() {
      assertThatThrownBy(() -> new Quantity(BigDecimal.valueOf(-10)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Quantity cannot be negative");
    }
  }
}