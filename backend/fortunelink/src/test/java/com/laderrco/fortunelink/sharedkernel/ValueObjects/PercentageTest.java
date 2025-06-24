package com.laderrco.fortunelink.sharedkernel.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Percentage;

public class PercentageTest {
    private Percentage percentage;
    @BeforeEach
    void init() {
        percentage = new Percentage(new BigDecimal(25));
    }

    @Test
    void testConstructor() {
        Percentage p2 = new Percentage(new BigDecimal(0).setScale(7));
        assertEquals(0, p2.percentValue().doubleValue());

        assertThrows(NullPointerException.class, ()->  new Percentage(null));
        assertThrows(IllegalArgumentException.class, ()->  new Percentage(new BigDecimal(-1)));
    }

    @Test
    void testEquals() {
        Percentage p2 = new Percentage(new BigDecimal(25));
        assertTrue(percentage.equals(p2));
        assertTrue(!percentage.equals(null));
    }

    @Test
    void fromPercent() {
        Percentage p2 = Percentage.fromPercent(new BigDecimal(12));
        assertEquals(1200.0, p2.percentValue().doubleValue());
    }

    @Test
    void testHashCode() {
        Percentage p2 = new Percentage(new BigDecimal(25));
        assertTrue(percentage.hashCode() == p2.hashCode());
    }

    @Test
    void testPercentValue() {
        BigDecimal expectedValue = new BigDecimal(25).setScale(6, RoundingMode.HALF_UP);
        assertEquals(expectedValue, percentage.percentValue());
    }

    @Test
    void testToString() {
        assertTrue(!percentage.toString().equals(null));
    }
}
