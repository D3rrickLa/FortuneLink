package com.laderrco.fortunelink.sharedkernel.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        assertTrue(percentage.equals(percentage));
        assertTrue(!percentage.equals(null));
        assertTrue(!percentage.equals(new Object()));
        assertTrue(!percentage.equals(new Percentage(new BigDecimal(250))));
    }

    @Test
    void testFromPercent() {
        Percentage p2 = Percentage.fromPercent(new BigDecimal(12));
        assertEquals(0.12, p2.percentValue().doubleValue());
    }
}
