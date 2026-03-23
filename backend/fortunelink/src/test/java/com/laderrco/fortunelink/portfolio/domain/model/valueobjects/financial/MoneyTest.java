package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.shared.enums.Precision;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Money Value Object Unit Tests")
class MoneyTest {
  private static final Currency USD = Currency.of("USD");

  @Nested
  @DisplayName("Creation and Validation")
  class CreationTests {
    @Test
    @DisplayName("constructor: success on valid amount and currency")
    void constructorInitializesCorrectly() {
      Money money = new Money(new BigDecimal("100.00"), USD);
      assertThat(money.amount()).isEqualByComparingTo("100.00");
      assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("constructor: fail on null amount")
    void constructorThrowsOnNullAmount() {
      assertThatThrownBy(() -> new Money(null, USD)).isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("factories: of methods create equivalent objects")
    void factoryMethodsProduceEquivalentResults() {
      Money fromDouble = Money.of(10.5, "USD");
      Money fromBigDecimal = Money.of(new BigDecimal("10.5"), "USD");

      assertThat(fromDouble).isEqualTo(fromBigDecimal);
    }
  }

  @Nested
  @DisplayName("Arithmetic Operations")
  class ArithmeticTests {
    @Test
    @DisplayName("add: success with same currency")
    void addSameCurrencySuccess() {
      Money m1 = Money.of(10, "USD");
      Money m2 = Money.of(20, "USD");

      assertThat(m1.add(m2).amount()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("add: fail on currency mismatch")
    void addDifferentCurrencyThrowsException() {
      Money m1 = Money.of(10, "USD");
      Money m2 = Money.of(10, "EUR");

      assertThatThrownBy(() -> m1.add(m2)).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("multiply: success with BigDecimal")
    void multiplyBigDecimalSuccess() {
      Money result = Money.of(10, "USD").multiply(new BigDecimal("1.555"));

      assertThat(result.currency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("multiply: success with Quantity")
    void multiplyQuantitySuccess() {
      Money result = Money.of(10, "USD").multiply(new Quantity(new BigDecimal("1.555")));

      assertThat(result.currency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("divide: success with BigDecimal")
    void divideBigDecimalSuccess() {
      Money result = Money.of(10, "USD").divide(BigDecimal.TWO);

      assertThat(result).isEqualTo(Money.of(5, "USD"));
    }

    @Test
    @DisplayName("divide: success with Quantity")
    void divideQuantitySuccess() {
      Money result = Money.of(10, "USD").divide(new Quantity(BigDecimal.TWO));

      assertThat(result).isEqualTo(Money.of(5, "USD"));
    }

    @Test
    @DisplayName("divide: fail on division by zero")
    void divideByZeroThrowsException() {
      assertThatThrownBy(() -> Money.of(10, "USD").divide(BigDecimal.ZERO)).isInstanceOf(
          ArithmeticException.class);
    }
  }

  @Nested
  @DisplayName("Comparison and Predicates")
  class ComparisonTests {
    @Test
    @DisplayName("isPositive: correct evaluation")
    void isPositiveCorrectEvaluation() {
      assertThat(Money.of(1, "USD").isPositive()).isTrue();
      assertThat(Money.of(0, "USD").isPositive()).isFalse();
    }

    @Test
    @DisplayName("compareTo: correct ordering")
    void compareToSameCurrencyOrdersCorrectly() {
      Money small = Money.of(10, "USD");
      Money large = Money.of(20, "USD");

      assertThat(small).isLessThan(large);
    }

    @Test
    @DisplayName("exceeds: correct evaluation")
    void exceedsSameCurrencyCorrectEvaluation() {
      Money small = Money.of(10, "USD");
      Money large = Money.of(20, "USD");

      assertThat(large.exceeds(small)).isTrue();
      assertThat(small.exceeds(large)).isFalse();
    }

    @Test
    @DisplayName("isAtLeast: correct evaluation")
    void isAtLeastSameCurrencyCorrectEvaluation() {
      Money base = Money.of(20, "USD");
      Money same = Money.of(20, "USD");
      Money larger = Money.of(21, "USD");

      assertThat(same.isAtLeast(base)).isTrue();
      assertThat(larger.isAtLeast(base)).isTrue();
      assertThat(base.isAtLeast(larger)).isFalse();
    }

    @Test
    @DisplayName("compareTo: fail on different currencies")
    void compareToDifferentCurrencyThrowsException() {
      Money m1 = Money.of(10, "USD");
      Money m2 = Money.of(10, "EUR");

      assertThatThrownBy(() -> m1.compareTo(m2)).isInstanceOf(CurrencyMismatchException.class);
    }
  }

  @Nested
  @DisplayName("Unary Operations")
  class UnaryTests {
    @Test
    @DisplayName("abs: removes negative sign")
    void absNegativeValueReturnsPositive() {
      Money result = Money.of(-50, "USD").abs();

      assertThat(result.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("negate: flips sign")
    void negatePositiveValueReturnsNegative() {
      Money result = Money.of(50, "USD").negate();

      assertThat(result.amount()).isEqualByComparingTo("-50.00");
    }
  }

  @Nested
  @DisplayName("Normalization and Scale Stability")
  class NormalizationTests {
    @Test
    @DisplayName("constructor: normalizes to global precision")
    void constructorNormalizesToGlobalPrecision() {
      Money money = new Money(new BigDecimal("10.5"), USD);

      assertThat(money.amount().scale()).isEqualTo(10);
      assertThat(money.amount()).isEqualByComparingTo("10.5");
    }

    @Test
    @DisplayName("multiply: maintains scale consistency")
    void multiplyMaintainsScaleConsistency() {
      Money result = Money.of(10, "USD").multiply(new BigDecimal("3.0"));

      assertThat(result.amount().scale()).isEqualTo(10);
    }
  }

  @Nested
  @DisplayName("Arithmetic Scale Enforcement")
  class ScaleEnforcementTests {
    @Test
    @DisplayName("add: maintains constant scale")
    void addMaintainsConstantScale() {
      Money m1 = new Money(new BigDecimal("10.1"), USD);
      Money m2 = new Money(new BigDecimal("20.222"), USD);

      Money result = m1.add(m2);

      assertThat(result.amount().scale()).isEqualTo(Precision.getMoneyPrecision());
    }

    @Test
    @DisplayName("multiply: prevents scale expansion")
    void multiplyPreventsScaleExpansion() {
      Money result = Money.of(10.00, "USD").multiply(new BigDecimal("1.123"));

      assertThat(result.amount().scale()).isEqualTo(Precision.getMoneyPrecision());
    }

    @Test
    @DisplayName("divide: uses internal rounding mode")
    void divideUsesInternalRoundingMode() {
      Money result = Money.of(10.00, "USD").divide(new BigDecimal("3"));

      assertThat(result.amount().scale()).isEqualTo(Precision.getMoneyPrecision());
      assertThat(result.amount()).isNotNull();
    }
  }
}