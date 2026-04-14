package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.shared.enums.Precision;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Money Value Object Unit Tests")
class MoneyTest {

  private static final Currency USD = Currency.of("USD");
  private static final Currency EUR = Currency.of("EUR");

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

    static Stream<Arguments> multiplicationProvider() {
      BigDecimal val = new BigDecimal("1.555");
      return Stream.of(Arguments.of(val), Arguments.of(new Quantity(val)));
    }

    static Stream<Arguments> divisionProvider() {
      return Stream.of(Arguments.of(BigDecimal.TWO), Arguments.of(new Quantity(BigDecimal.TWO)));
    }

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
      Money usd = Money.of(10, "USD");
      Money eur = Money.of(10, EUR);

      assertThatThrownBy(() -> usd.add(eur)).isInstanceOf(CurrencyMismatchException.class);
    }

    @ParameterizedTest(name = "multiply by {0}")
    @MethodSource("multiplicationProvider")
    @DisplayName("multiply: success with different factor types")
    void multiplyCalculatesCorrectResult(Object factor) {
      Money base = Money.of(10, "USD");
      Money result =
          (factor instanceof BigDecimal b) ? base.multiply(b) : base.multiply((Quantity) factor);

      assertThat(result.amount()).isEqualByComparingTo("15.55");
      assertThat(result.currency()).isEqualTo(USD);
    }

    @ParameterizedTest(name = "divide by {0}")
    @MethodSource("divisionProvider")
    @DisplayName("divide: success with different divisor types")
    void divideCalculatesCorrectResult(Object divisor) {
      Money base = Money.of(10, "USD");
      Money result =
          (divisor instanceof BigDecimal b) ? base.divide(b) : base.divide((Quantity) divisor);

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

    static Stream<Arguments> positiveProvider() {
      return Stream.of(Arguments.of(1.0, true), Arguments.of(0.0, false),
          Arguments.of(-1.0, false));
    }

    @ParameterizedTest(name = "value {0} isPositive should be {1}")
    @MethodSource("positiveProvider")
    @DisplayName("isPositive: correct evaluation")
    void isPositiveCorrectEvaluation(double amount, boolean expected) {
      assertThat(Money.of(amount, "USD").isPositive()).isEqualTo(expected);
    }

    @Test
    @DisplayName("compareTo: correct ordering")
    void compareToSameCurrencyOrdersCorrectly() {
      assertThat(Money.of(10, "USD")).isLessThan(Money.of(20, "USD"));
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

      assertThat(Money.of(20, "USD").isAtLeast(base)).isTrue();
      assertThat(Money.of(21, "USD").isAtLeast(base)).isTrue();
      assertThat(Money.of(19, "USD").isAtLeast(base)).isFalse();
    }

    @Test
    @DisplayName("compareTo: fail on different currencies")
    void compareToDifferentCurrencyThrowsException() {
      Money usd = Money.of(10, "USD");
      Money eur = Money.of(10, EUR);

      assertThatThrownBy(() -> usd.compareTo(eur)).isInstanceOf(CurrencyMismatchException.class);
    }
  }

  @Nested
  @DisplayName("Unary Operations")
  class UnaryTests {

    @Test
    @DisplayName("abs: removes negative sign")
    void absNegativeValueReturnsPositive() {
      assertThat(Money.of(-50, "USD").abs().amount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("negate: flips sign")
    void negatePositiveValueReturnsNegative() {
      assertThat(Money.of(50, "USD").negate().amount()).isEqualByComparingTo("-50.00");
    }
  }

  @Nested
  @DisplayName("Normalization and Scale Stability")
  class ScaleTests {

    static Stream<Arguments> operationProvider() {
      Money m1 = Money.of(10.1, "USD");
      return Stream.of(Arguments.of(new Money(new BigDecimal("10.5"), USD)),
          Arguments.of(m1.add(Money.of(20.222, "USD"))),
          Arguments.of(m1.multiply(new BigDecimal("1.123"))),
          Arguments.of(m1.divide(new BigDecimal("3"))));
    }

    @ParameterizedTest(name = "operation: {0}")
    @MethodSource("operationProvider")
    @DisplayName("arithmetic: maintains global precision scale")
    void arithmeticOperationsMaintainConstantScale(Money result) {
      assertThat(result.amount().scale()).isEqualTo(Precision.getMoneyPrecision());
    }
  }
}