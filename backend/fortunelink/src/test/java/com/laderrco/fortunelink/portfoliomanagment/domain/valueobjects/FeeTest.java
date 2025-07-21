package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.shared.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class FeeTest {

    @Test 
    void testConstructor() {
        Fee fee = new Fee(FeeType.ACCOUNT_MAINTENANCE, new Money(new BigDecimal("12.54"), Currency.getInstance("USD")));
        assertEquals( Currency.getInstance("USD"), fee.amount().currency());
        assertEquals(2, fee.amount().currency().getDefaultFractionDigits());
    }

    @Test
    void testConstructorInValidFeeNegative() {
        assertThrows(InvalidQuantityException.class, () -> new Fee(FeeType.ACCOUNT_MAINTENANCE, new Money(new BigDecimal("-12.54"), Currency.getInstance("USD"))));
    }
}
