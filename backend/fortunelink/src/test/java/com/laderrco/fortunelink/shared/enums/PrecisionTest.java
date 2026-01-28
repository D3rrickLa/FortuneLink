package com.laderrco.fortunelink.shared.enums;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class PrecisionTest {
    @Test
    void testFromAssetType() {
        assertThrows(UnsupportedOperationException.class, () -> Precision.fromAssetType(getClass()));
    }

    @Test
    void testGetDecimalPlaces() {

    }

    @Test
    void testGetMoneyPrecision() {

    }

    @Test
    void testValueOf() {

    }

    @Test
    void testValues() {

    }
}
