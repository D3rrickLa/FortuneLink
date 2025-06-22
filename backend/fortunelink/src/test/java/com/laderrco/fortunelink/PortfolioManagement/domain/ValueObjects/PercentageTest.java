package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class PercentageTest {
    @Test
    void testEquals() {
        Percentage p1 = new Percentage(new BigDecimal(100));
        Percentage p2 = new Percentage(new BigDecimal(100));

        assertTrue(p1.equals(p2));
    }

    @Test
    void testHashCode() {
        Percentage p1 = new Percentage(new BigDecimal(100));
        Percentage p2 = new Percentage(new BigDecimal(100));

        assertTrue(p1.hashCode() == p2.hashCode());
    }

    @Test
    void testToString() {
        Percentage p1 = new Percentage(new BigDecimal(100));
        Percentage p2 = new Percentage(new BigDecimal(100));

        assertTrue(p1.toString().equals(p2.toString()));
    }

    @Test
    void testValue() {
        Percentage p1 = new Percentage(new BigDecimal(100));
        Percentage p2 = new Percentage(new BigDecimal(100));

        assertTrue(p1.value().compareTo(p2.value()) == 0);
    }

    @Test
    void testValueLessThan0() {
        assertThrows(IllegalArgumentException.class, () -> new Percentage(new BigDecimal(-20)));
    }

    @Test
    void testValueScalingTo4Decimals() {
        Percentage p1 = new Percentage(new BigDecimal("100.50"));
        assertTrue(p1.value().scale() == 4);
    }

    @Test
    void testValueScalingGreaterThan4Decimals() {
        Percentage p1 = new Percentage(new BigDecimal("100.500001"));
        assertTrue(p1.value().scale() == 6);
    }

    @Test 
    void testFromBigDecimal() {
        BigDecimal decimal = new BigDecimal("100.0");
        Percentage x = Percentage.fromBigDecimal(decimal);
        assertEquals(1.000000D, x.value().doubleValue());
    }
}
