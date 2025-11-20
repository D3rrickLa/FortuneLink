package com.laderrco.fortunelink.shared.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
