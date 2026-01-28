package com.laderrco.fortunelink.shared.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

public class PercentageTest {
    private Percentage testPercentage;

    @BeforeEach
    void setup() {
        testPercentage = new Percentage(BigDecimal.valueOf(25));
    }

    @Test
    void testOfMethods() {
        BigDecimal testValue = BigDecimal.valueOf(25);
        Percentage percentageTest01 = Percentage.of(25d);
        Percentage percentageTest02 = Percentage.of(testValue);

        assertEquals(testPercentage, percentageTest01);
        assertEquals(testPercentage, percentageTest02);
    }

    @Test
    void testOfFromPercentageMethods() {
        BigDecimal testValue = BigDecimal.valueOf(25);
        Percentage percentageTest01 = Percentage.fromPercentage(25d);
        Percentage percentageTest02 = Percentage.fromPercentage(testValue);

        Percentage expectedValue = Percentage.of(0.25d);
        assertEquals(expectedValue, percentageTest01);
        assertEquals(expectedValue, percentageTest02);
    }

    @Test
    void testToPercentage() {
        BigDecimal testValue = BigDecimal.valueOf(25);
        Percentage percentageTest01 = Percentage.fromPercentage(25d);
        Percentage percentageTest02 = Percentage.fromPercentage(testValue);

        BigDecimal expectedValue = Percentage.of(0.25d).toPercentage();
        assertEquals(expectedValue, percentageTest01.toPercentage());
        assertEquals(expectedValue, percentageTest02.toPercentage()); 
    }

    @Test 
    void testCompareTo() {
        BigDecimal testValue = BigDecimal.valueOf(23);
        Percentage percentageTest01 = Percentage.of(25d);
        Percentage percentageTest02 = Percentage.of(testValue);

        assertTrue(testPercentage.compareTo(percentageTest01) == 0);
        assertTrue(testPercentage.compareTo(percentageTest02) == 1);
    }

    @Test
    void testAddPercentage() {
        Percentage percentageTest01 = Percentage.of(25d);
        Percentage expectedValue = Percentage.of(50d);
        Percentage actualPercentage = testPercentage.addPercentage(percentageTest01);
        assertEquals(expectedValue, actualPercentage);
    }

    @Test
    void testMultiplePercentage() {
        Percentage percentageTest01 = Percentage.of(2d);
        Percentage expectedValue = Percentage.of(50d);
        Percentage actualPercentage = testPercentage.multiplyPercentage(percentageTest01);
        assertEquals(expectedValue, actualPercentage);
    }

    @ParameterizedTest
    @ValueSource(doubles  = {2, 1, 12})
    void testAnnualize(double years){
        Percentage expectedValue = new Percentage(BigDecimal.valueOf(1).divide(BigDecimal.valueOf(years), Precision.PERCENTAGE.getDecimalPlaces(), Rounding.PERCENTAGE.getMode()));
        Percentage actual = new Percentage(BigDecimal.valueOf(1)).annualize(years);
        assertEquals(expectedValue, actual);
    }

    @ParameterizedTest
    @ValueSource(doubles  = {0, -0, -12, Double.NEGATIVE_INFINITY})
    void testAnnualizeThrowErrorsForNegativeAndZero(double years){
        Percentage testPercentage = new Percentage(BigDecimal.valueOf(1));
        assertThrows(IllegalArgumentException.class, () -> testPercentage.annualize(years));
    }

}
