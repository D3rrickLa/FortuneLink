package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import org.junit.jupiter.api.*;


import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@DisplayName("PercentageRate Value Object Unit Tests")
class PercentageRateTest {

    @Nested
    @DisplayName("Creation and Constraints")
    class CreationTests {

        @Test
        @DisplayName("constructor_success_ValidRate")
        void constructor_success_ValidRate() {
            BigDecimal valid = new BigDecimal("0.05");
            PercentageRate pr = new PercentageRate(valid);
            assertThat(pr.rate()).isEqualByComparingTo("0.05");
        }

        @Test
        @DisplayName("constructor_fail_NegativeRate")
        void constructor_fail_NegativeRate() {
            assertThatThrownBy(() -> new PercentageRate(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
        }

        @Test
        @DisplayName("fromPercent_success_ConvertsCorrectly")
        void fromPercent_success_ConvertsCorrectly() {
            // 5.5% should be 0.055
            PercentageRate pr = PercentageRate.fromPercent(new BigDecimal("5.5"));
            assertThat(pr.rate()).isEqualByComparingTo("0.055");
        }

        @Test
        @DisplayName("fromRate_success_FactoryMethod")
        void fromRate_success_FactoryMethod() {
            PercentageRate pr = PercentageRate.fromRate(new BigDecimal("0.12"));
            assertThat(pr.rate()).isEqualByComparingTo("0.12");
        }
    }

    @Nested
    @DisplayName("Arithmetic and Compounding")
    class ArithmeticTests {

        @Test
        @DisplayName("add_success_Summation")
        void add_success_Summation() {
            PercentageRate pr1 = PercentageRate.fromRate(new BigDecimal("0.05"));
            PercentageRate pr2 = PercentageRate.fromRate(new BigDecimal("0.02"));
            
            assertThat(pr1.add(pr2).rate()).isEqualByComparingTo("0.07");
        }

        @Test
        @DisplayName("compoundWith_success_Multiplication")
        void compoundWith_success_Multiplication() {
            PercentageRate pr1 = PercentageRate.fromRate(new BigDecimal("0.10"));
            PercentageRate pr2 = PercentageRate.fromRate(new BigDecimal("0.10"));
            
            // 0.10 * 0.10 = 0.01
            assertThat(pr1.compoundWith(pr2).rate()).isEqualByComparingTo("0.01");
        }

        @Test
        @DisplayName("toPercent_success_ReciprocalCalculation")
        void toPercent_success_ReciprocalCalculation() {
            PercentageRate pr = PercentageRate.fromRate(new BigDecimal("0.075"));
            assertThat(pr.toPercent()).isEqualByComparingTo("7.5");
        }
    }

    @Nested
    @DisplayName("Time-Based Calculations")
    class TimeTests {

        @Test
        @DisplayName("annualizedOver_success_ValidCalculation")
        void annualizedOver_success_ValidCalculation() {
            // A 21% total return over 2 years: (1.21)^(1/2) - 1 = 0.10 (10% annual)
            PercentageRate totalReturn = PercentageRate.fromRate(new BigDecimal("0.21"));
            PercentageRate annualized = totalReturn.annualizedOver(2.0);
            
            assertThat(annualized.rate()).isEqualByComparingTo("0.10");
        }

        @Test
        @DisplayName("annualizedOver_fail_NonPositiveYears")
        void annualizedOver_fail_NonPositiveYears() {
            PercentageRate pr = PercentageRate.fromRate(new BigDecimal("0.10"));
            
            assertThatThrownBy(() -> pr.annualizedOver(0))
                .isInstanceOf(IllegalArgumentException.class);
                
            assertThatThrownBy(() -> pr.annualizedOver(-1))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Comparisons")
    class ComparisonTests {

        @Test
        @DisplayName("compareTo_success_StandardOrdering")
        void compareTo_success_StandardOrdering() {
            PercentageRate lower = PercentageRate.fromRate(new BigDecimal("0.05"));
            PercentageRate higher = PercentageRate.fromRate(new BigDecimal("0.10"));
            
            assertThat(lower).isLessThan(higher);
            assertThat(higher).isEqualByComparingTo(PercentageRate.fromRate(new BigDecimal("0.10")));
        }
    }
}