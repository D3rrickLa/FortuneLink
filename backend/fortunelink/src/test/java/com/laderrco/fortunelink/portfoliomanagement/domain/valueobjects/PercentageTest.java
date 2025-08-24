package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.Rounding;

import static org.assertj.core.api.Assertions.*;

class PercentageTest {

    // --- Constructor Tests ---
    @Test
    void constructor_shouldCreatePercentage() {
        Percentage p = new Percentage(BigDecimal.valueOf(0.25));
        assertThat(p.value()).isEqualByComparingTo(BigDecimal.valueOf(0.25).setScale(DecimalPrecision.PERCENTAGE.getDecimalPlaces(), Rounding.PERCENTAGE.getMode()));
    }

    @Test
    void constructor_shouldThrowWhenNull() {
        assertThatNullPointerException()
            .isThrownBy(() -> new Percentage(null))
            .withMessage("Value cannot be null.");
    }

    // --- Factory Methods ---
    @Test
    void of_double_shouldCreatePercentage() {
        Percentage p = Percentage.of(0.33);
        assertThat(p.value()).isEqualByComparingTo(BigDecimal.valueOf(0.33).setScale(DecimalPrecision.PERCENTAGE.getDecimalPlaces(), Rounding.PERCENTAGE.getMode()));
    }

    @Test
    void of_BigDecimal_shouldCreatePercentage() {
        Percentage p = Percentage.of(BigDecimal.valueOf(0.77));
        assertThat(p.value()).isEqualByComparingTo(BigDecimal.valueOf(0.77).setScale(DecimalPrecision.PERCENTAGE.getDecimalPlaces(), Rounding.PERCENTAGE.getMode()));
    }

    @Test
    void fromPercentage_shouldConvertFromHumanReadablePercent() {
        BigDecimal humanPercent = BigDecimal.valueOf(45);
        Percentage p = Percentage.fromPercentage(humanPercent);
        BigDecimal expected = humanPercent.divide(BigDecimal.valueOf(100), DecimalPrecision.PERCENTAGE.getDecimalPlaces(), Rounding.PERCENTAGE.getMode());
        assertThat(p.value()).isEqualByComparingTo(expected);
    }

    @Test
    void fromPercentage_shouldThrowWhenNull() {
        assertThatNullPointerException()
            .isThrownBy(() -> Percentage.fromPercentage(null))
            .withMessageContaining("Percent cannot be null.");
    }

    // --- toPercentage ---
    @Test
    void toPercentage_shouldReturnHumanReadableValue() {
        Percentage p = new Percentage(BigDecimal.valueOf(0.45));
        BigDecimal percent = p.toPercentage();
        assertThat(percent).isEqualByComparingTo(BigDecimal.valueOf(45));
    }

    // --- compareTo ---
    @Test
    void compareTo_shouldReturnZeroForEqualPercentages() {
        Percentage a = new Percentage(BigDecimal.valueOf(0.25));
        Percentage b = new Percentage(BigDecimal.valueOf(0.25));
        assertThat(a.compareTo(b)).isZero();
    }

    @Test
    void compareTo_shouldReturnNegativeWhenSmaller() {
        Percentage a = new Percentage(BigDecimal.valueOf(0.10));
        Percentage b = new Percentage(BigDecimal.valueOf(0.20));
        assertThat(a.compareTo(b)).isNegative();
    }

    @Test
    void compareTo_shouldReturnPositiveWhenGreater() {
        Percentage a = new Percentage(BigDecimal.valueOf(0.75));
        Percentage b = new Percentage(BigDecimal.valueOf(0.50));
        assertThat(a.compareTo(b)).isPositive();
    }

    @Test
    void compareTo_shouldThrowWhenNull() {
        Percentage a = new Percentage(BigDecimal.valueOf(0.1));
        assertThatNullPointerException()
            .isThrownBy(() -> a.compareTo(null))
            .withMessage("Percentage to compare cannot be null.");
    }

    // --- Boundary Tests ---
    @Test
    void boundary_zeroPercent() {
        Percentage p = new Percentage(BigDecimal.ZERO);
        assertThat(p.toPercentage()).isZero();
    }

    @Test
    void boundary_hundredPercent() {
        Percentage p = Percentage.fromPercentage(BigDecimal.valueOf(100));
        assertThat(p.value()).isEqualByComparingTo(BigDecimal.ONE.setScale(DecimalPrecision.PERCENTAGE.getDecimalPlaces(), Rounding.PERCENTAGE.getMode()));
        assertThat(p.toPercentage()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void boundary_roundingTest() {
        BigDecimal input = new BigDecimal("0.123456789");
        Percentage p = new Percentage(input);
        BigDecimal expected = input.setScale(DecimalPrecision.PERCENTAGE.getDecimalPlaces(), Rounding.PERCENTAGE.getMode());
        assertThat(p.value()).isEqualByComparingTo(expected);
    }

}