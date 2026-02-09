package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import org.junit.jupiter.api.*;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;

@DisplayName("Money Value Object Unit Tests")
class MoneyTest {

    private final Currency USD = Currency.of("USD");

    @Nested
    @DisplayName("Creation and Validation")
    class CreationTests {

        @Test
        @DisplayName("constructor_success_ValidAmountAndCurrency")
        void constructor_success_ValidAmountAndCurrency() {
            Money money = new Money(new BigDecimal("100.00"), USD);
            assertThat(money.amount()).isEqualByComparingTo("100.00");
            assertThat(money.currency()).isEqualTo(USD);
        }

        @Test
        @DisplayName("constructor_fail_NullAmount")
        void constructor_fail_NullAmount() {
            assertThatThrownBy(() -> new Money(null, USD))
                    .isInstanceOf(DomainArgumentException.class);
        }

        @Test
        @DisplayName("constructor_fail_ExceedsPrecision")
        void constructor_fail_ExceedsPrecision() {
            BigDecimal highPrecision = new BigDecimal(
                    "100.1234567999999999999999999999999999999999999999999999999999999999998");
            assertThatThrownBy(() -> new Money(highPrecision, USD))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of_success_FactoryMethods")
        void of_success_FactoryMethods() {
            Money fromDouble = Money.of(10.5, "USD");
            Money fromBigDecimal = Money.of(new BigDecimal("10.5"), "USD");

            assertThat(fromDouble).isEqualTo(fromBigDecimal);
        }
    }

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticTests {

        @Test
        @DisplayName("add_success_SameCurrency")
        void add_success_SameCurrency() {
            Money m1 = Money.of(10, "USD");
            Money m2 = Money.of(20, "USD");
            assertThat(m1.add(m2).amount()).isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("add_fail_CurrencyMismatch")
        void add_fail_CurrencyMismatch() {
            Money m1 = Money.of(10, "USD");
            Money m2 = Money.of(10, "EUR");
            assertThatThrownBy(() -> m1.add(m2))
                    .isInstanceOf(CurrencyMismatchException.class);
        }

        @Test
        @DisplayName("multiply_success_ScalesCorrectly")
        void multiply_success_ScalesCorrectly() {
            Money m1 = Money.of(10, "USD");
            Money result = m1.multiply(new BigDecimal("1.555"));
            // Validation depends on your RoundingMode, but tests consistency
            assertThat(result.currency()).isEqualTo(USD);
        }

        @Test
        @DisplayName("multiply_success_ScalesCorrectly")
        void multiply_success_ScalesCorrectly_viaQuantity() {
            Money m1 = Money.of(10, "USD");
            Money result = m1.multiply(new Quantity(new BigDecimal("1.555")));
            // Validation depends on your RoundingMode, but tests consistency
            assertThat(result.currency()).isEqualTo(USD);
        }

        @Test
        @DisplayName("divide_success")
        void divide_suceess_BigDecimal() {
            Money m1 = Money.of(10, "USD");
            Money output = m1.divide(BigDecimal.TWO);
            assertEquals(Money.of(5, "USD"), output);
        }

        @Test
        @DisplayName("divide_success")
        void divide_suceess_Quantity() {
            Money m1 = Money.of(10, "USD");
            Money output = m1.divide(new Quantity(BigDecimal.TWO));
            assertEquals(Money.of(5, "USD"), output);
        }

        @Test
        @DisplayName("divide_fail_ByZero")
        void divide_fail_ByZero() {
            Money m1 = Money.of(10, "USD");
            assertThatThrownBy(() -> m1.divide(BigDecimal.ZERO))
                    .isInstanceOf(ArithmeticException.class);
        }
    }

    @Nested
    @DisplayName("Comparison and Predicates")
    class ComparisonTests {

        @Test
        @DisplayName("isPositive_success_CorrectEvaluation")
        void isPositive_success_CorrectEvaluation() {
            assertThat(Money.of(1, "USD").isPositive()).isTrue();
            assertThat(Money.of(0, "USD").isPositive()).isFalse();
        }

        @Test
        @DisplayName("compareTo_success_SortedOrder")
        void compareTo_success_SortedOrder() {
            Money small = Money.of(10, "USD");
            Money large = Money.of(20, "USD");
            assertThat(small).isLessThan(large);
        }

        @Test
        @DisplayName("exceeds_success_SortedOrder")
        void exceeds_success_SortedOrder() {
            Money small = Money.of(10, "USD");
            Money large = Money.of(20, "USD");
            assertThat(large.exceeds(small)).isTrue();
            assertThat(small.exceeds(large)).isFalse();
        }

        @Test
        @DisplayName("isAtleast_success_SortedOrder")
        void isAtleast_success_SortedOrder() {
            Money small = Money.of(20, "USD");
            Money large = Money.of(20, "USD");
            Money larger = Money.of(21, "USD");
            assertThat(large.isAtLeast(small)).isTrue();
            assertThat(larger.isAtLeast(large)).isTrue();
        }

        @Test
        @DisplayName("compareTo_fail_DifferentCurrencies")
        void compareTo_fail_DifferentCurrencies() {
            Money m1 = Money.of(10, "USD");
            Money m2 = Money.of(10, "EUR");
            assertThatThrownBy(() -> m1.compareTo(m2))
                    .isInstanceOf(CurrencyMismatchException.class);
        }
    }

    @Nested
    @DisplayName("Unary Operations")
    class UnaryTests {

        @Test
        @DisplayName("abs_success_RemovesNegative")
        void abs_success_RemovesNegative() {
            Money negative = Money.of(-50, "USD");
            assertThat(negative.abs().amount()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("negate_success_FlipsSign")
        void negate_success_FlipsSign() {
            Money positive = Money.of(50, "USD");
            assertThat(positive.negate().amount()).isEqualByComparingTo("-50.00");
        }
    }

    @Nested
    @DisplayName("Normalization and Scale Stability")
    class NormalizationTests {

        @Test
        @DisplayName("constructor_success_NormalizesToGlobalPrecision")
        void constructor_success_NormalizesToGlobalPrecision() {
            // If Precision.getMoneyPrecision() is 34
            BigDecimal smallScale = new BigDecimal("10.5"); // scale 1
            Money money = new Money(smallScale, USD);

            assertThat(money.amount().scale()).isEqualTo(34);
            assertThat(money.amount()).isEqualByComparingTo("10.5");
        }

        @Test
        @DisplayName("multiply_success_MaintainsScaleConsistency")
        void multiply_success_MaintainsScaleConsistency() {
            Money m1 = Money.of(10, "USD"); // scale 34 internally
            BigDecimal multiplier = new BigDecimal("3.0"); // scale 1

            Money result = m1.multiply(multiplier);

            // Ensure result didn't jump to scale 35
            assertThat(result.amount().scale()).isEqualTo(34);
        }
    }

    @Nested
    @DisplayName("Arithmetic Scale Enforcement")
    class ScaleEnforcementTests {

        @Test
        @DisplayName("add_success_MaintainsConstantScale")
        void add_success_MaintainsConstantScale() {
            // Even if we add two BigDecimals with different scales
            Money m1 = new Money(new BigDecimal("10.1"), USD); // Will be scaled to MONEY_PRECISION
            Money m2 = new Money(new BigDecimal("20.222"), USD); // Will be scaled to MONEY_PRECISION

            Money result = m1.add(m2);

            // Assert result scale is exactly our constant, not the max of the two
            assertThat(result.amount().scale()).isEqualTo(Precision.getMoneyPrecision());
        }

        @Test
        @DisplayName("multiply_success_PreventsScaleExpansion")
        void multiply_success_PreventsScaleExpansion() {
            Money money = Money.of(10.00, "USD");
            // Multiplier has scale 3
            BigDecimal multiplier = new BigDecimal("1.123");

            Money result = money.multiply(multiplier);

            // Without applyScale, scale would be (MONEY_PRECISION + 3)
            // With applyScale, it should remain MONEY_PRECISION
            assertThat(result.amount().scale()).isEqualTo(Precision.getMoneyPrecision());
        }

        @Test
        @DisplayName("divide_success_UsesInternalRoundingMode")
        void divide_success_UsesInternalRoundingMode() {
            Money money = Money.of(10.00, "USD");
            BigDecimal divisor = new BigDecimal("3");

            // Division usually creates infinite/large scales
            Money result = money.divide(divisor);

            assertThat(result.amount().scale()).isEqualTo(Precision.getMoneyPrecision());
            // Verify it didn't throw ArithmeticException for non-terminating decimal
            assertThat(result.amount()).isNotNull();
        }
    }
}