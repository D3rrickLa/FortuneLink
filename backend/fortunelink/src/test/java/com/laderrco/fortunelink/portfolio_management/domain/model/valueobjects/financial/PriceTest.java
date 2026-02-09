package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class PriceTest {
    @Test
    void testConstructor_Success() {
        Price price = new Price(Money.of(25, "USD"));
        assertNotNull(price);
    }

    @Test
    void testConstructor_fail_priceNegative() {
        assertThatThrownBy(() -> new Price(Money.of(-25, "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");

    }

    @Test
    void testCalculateValue_successful() {
        Price price = new Price(Money.of(25, "USD"));
        Money actual = price.calculateValue(new Quantity(BigDecimal.TEN));
        assertEquals(Money.of(250, "USD"), actual);
    }
}
