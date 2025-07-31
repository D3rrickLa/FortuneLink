package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;

public class PercentageTest {

    private final static int SCALE = DecimalPrecision.PERCENTAGE.getDecimalPlaces();

    @Test
    void constructor_shouldSetValueCorrectly_whenValidInput() {
        BigDecimal input = BigDecimal.valueOf(0.12345600);
        Percentage percentage = new Percentage(input);
        assertEquals(input.setScale(SCALE), percentage.toDecimal());
    }

    @Test
    void constructor_shouldThrowException_whenNullValue() {
        assertThrows(NullPointerException.class, () -> new Percentage(null));
    }

    @Test
    void constructor_shouldThrowException_whenNegativeValue() {
        BigDecimal negative = BigDecimal.valueOf(-0.01);
        assertThrows(IllegalArgumentException.class, () -> new Percentage(negative));
    }

    @Test
    void fromPercentage_shouldConvertCorrectly() {
        BigDecimal percent = BigDecimal.valueOf(56.78);
        Percentage percentage = Percentage.fromPercentage(percent);
        assertEquals(BigDecimal.valueOf(0.567800).setScale(SCALE), percentage.toDecimal());
    }

    @Test
    void fromDecimal_shouldSetScaleCorrectly() {
        BigDecimal decimal = BigDecimal.valueOf(0.987654321);
        Percentage percentage = Percentage.fromDecimal(decimal);
        assertEquals(BigDecimal.valueOf(0.98765432).setScale(SCALE), percentage.toDecimal());
    }

    @Test
    void toDecimal_shouldReturnValueAtScale() {
        Percentage percentage = new Percentage(BigDecimal.valueOf(0.123456));
        assertEquals(BigDecimal.valueOf(0.123456).setScale(SCALE), percentage.toDecimal());
    }

    @Test
    void toPercentage_shouldReturnCorrectPercentageFormat() {
        Percentage percentage = new Percentage(BigDecimal.valueOf(0.56));
        assertEquals(BigDecimal.valueOf(56.000000).setScale(SCALE), percentage.toPercentage());
    }

    @Test
    void compareTo_shouldReturnZeroForEqualValues() {
        Percentage p1 = new Percentage(BigDecimal.valueOf(0.123456));
        Percentage p2 = new Percentage(BigDecimal.valueOf(0.123456));
        assertEquals(0, p1.compareTo(p2));
    }

    @Test
    void compareTo_shouldReturnPositiveIfGreater() {
        Percentage p1 = new Percentage(BigDecimal.valueOf(0.9));
        Percentage p2 = new Percentage(BigDecimal.valueOf(0.5));
        assertTrue(p1.compareTo(p2) > 0);
    }

    @Test
    void compareTo_shouldReturnNegativeIfLess() {
        Percentage p1 = new Percentage(BigDecimal.valueOf(0.2));
        Percentage p2 = new Percentage(BigDecimal.valueOf(0.5));
        assertTrue(p1.compareTo(p2) < 0);
    }

    @Test
    void of_shouldCreateCorrectPercentageFromDouble() {
        Percentage percentage = Percentage.of(0.123456);
        assertEquals(BigDecimal.valueOf(0.123456).setScale(SCALE), percentage.toDecimal());
    }
}
