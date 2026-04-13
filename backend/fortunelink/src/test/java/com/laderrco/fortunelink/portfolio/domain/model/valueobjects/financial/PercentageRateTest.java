package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PercentageRate Value Object Unit Tests")
class PercentageRateTest {
  @Nested
  @DisplayName("Creation and Constraints")
  class CreationTests {
    @Test
    @DisplayName("constructor: success on valid rate and factory methods")
    void factoryMethodsCreateCorrectRates() {
      BigDecimal rateValue = new BigDecimal("0.05");
      PercentageRate pr = new PercentageRate(rateValue);
      PercentageRate fromPercent = PercentageRate.fromPercent(new BigDecimal("5.5"));
      PercentageRate fromRate = PercentageRate.fromRate(new BigDecimal("0.12"));

      assertThat(pr.rate()).isEqualByComparingTo("0.05");
      assertThat(fromPercent.rate()).isEqualByComparingTo("0.055");
      assertThat(fromRate.rate()).isEqualByComparingTo("0.12");
    }

    @Test
    @DisplayName("constructor: fail when rate is negative")
    void constructorThrowsOnNegativeRate() {
      assertThatThrownBy(() -> new PercentageRate(new BigDecimal("-0.01"))).isInstanceOf(
          IllegalArgumentException.class).hasMessageContaining("cannot be negative");
    }
  }

  @Nested
  @DisplayName("Arithmetic and Compounding")
  class ArithmeticTests {
    @Test
    @DisplayName("arithmetic: success on sum and compound calculations")
    void arithmeticOperationsPerformCorrectMath() {
      PercentageRate pr1 = PercentageRate.fromRate(new BigDecimal("0.05"));
      PercentageRate pr2 = PercentageRate.fromRate(new BigDecimal("0.02"));
      PercentageRate compound1 = PercentageRate.fromRate(new BigDecimal("0.10"));
      PercentageRate compound2 = PercentageRate.fromRate(new BigDecimal("0.10"));

      assertThat(pr1.add(pr2).rate()).isEqualByComparingTo("0.07");
      assertThat(compound1.compoundWith(compound2).rate()).isEqualByComparingTo("0.01");
      assertThat(pr1.toPercent()).isEqualByComparingTo("5.0");
    }
  }

  @Nested
  @DisplayName("Time-Based Calculations")
  class TimeTests {
    @Test
    @DisplayName("annualizedOver: success on valid calculation")
    void annualizationCalculatesCorrectRate() {
      
      PercentageRate totalReturn = PercentageRate.fromRate(new BigDecimal("0.21"));
      PercentageRate annualized = totalReturn.annualizedOver(BigDecimal.valueOf(2.0));

      assertThat(annualized.rate()).isEqualByComparingTo("0.10");
    }

    @Test
    @DisplayName("annualizedOver: fail on non-positive years")
    void annualizationThrowsOnInvalidYears() {
      PercentageRate pr = PercentageRate.fromRate(new BigDecimal("0.10"));

      assertThatThrownBy(() -> pr.annualizedOver(BigDecimal.ZERO)).isInstanceOf(
          IllegalArgumentException.class);

      assertThatThrownBy(() -> pr.annualizedOver(BigDecimal.valueOf(-1))).isInstanceOf(
          IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("Comparisons")
  class ComparisonTests {
    @Test
    @DisplayName("compareTo: success on standard ordering")
    void ratesOrderCorrectly() {
      PercentageRate lower = PercentageRate.fromRate(new BigDecimal("0.05"));
      PercentageRate higher = PercentageRate.fromRate(new BigDecimal("0.10"));

      assertThat(lower).isLessThan(higher);
      assertThat(higher).isEqualByComparingTo(PercentageRate.fromRate(new BigDecimal("0.10")));
    }
  }
}