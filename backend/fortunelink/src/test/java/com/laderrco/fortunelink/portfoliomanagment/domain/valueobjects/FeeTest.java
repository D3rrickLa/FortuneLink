package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;

public class FeeTest {
    private final Currency USD = Currency.getInstance("USD");
    private final Money VALID_AMOUNT = new Money(BigDecimal.valueOf(100), USD);
    private final FeeType VALID_TYPE = FeeType.TRANSACTION_FEE; // Example enum value
    private final String VALID_DESCRIPTION = "Processing fee";
    private final Instant VALID_TIME = Instant.now();

    @Test
    void constructor_shouldCreateValidFee() {
        Fee fee = new Fee(VALID_TYPE, VALID_AMOUNT, VALID_DESCRIPTION, VALID_TIME);
        assertEquals(VALID_TYPE, fee.type());
        assertEquals(VALID_AMOUNT, fee.amount());
        assertEquals(VALID_DESCRIPTION, fee.description());
        assertEquals(VALID_TIME, fee.time());
    }

    @Test
    void constructor_shouldThrowException_whenTypeIsNull() {
        assertThrows(NullPointerException.class, () ->
            new Fee(null, VALID_AMOUNT, VALID_DESCRIPTION, VALID_TIME)
        );
    }

    @Test
    void constructor_shouldThrowException_whenAmountIsNull() {
        assertThrows(NullPointerException.class, () ->
            new Fee(VALID_TYPE, null, VALID_DESCRIPTION, VALID_TIME)
        );
    }

    @Test
    void constructor_shouldThrowException_whenDescriptionIsNull() {
        assertThrows(NullPointerException.class, () ->
            new Fee(VALID_TYPE, VALID_AMOUNT, null, VALID_TIME)
        );
    }

    @Test
    void constructor_shouldThrowException_whenTimeIsNull() {
        assertThrows(NullPointerException.class, () ->
            new Fee(VALID_TYPE, VALID_AMOUNT, VALID_DESCRIPTION, null)
        );
    }

    @Test
    void constructor_shouldThrowInvalidQuantityException_whenAmountIsZero() {
        Money zeroAmount = new Money(BigDecimal.ZERO, USD);
        assertThrows(InvalidQuantityException.class, () ->
            new Fee(VALID_TYPE, zeroAmount, VALID_DESCRIPTION, VALID_TIME)
        );
    }

    @Test
    void constructor_shouldThrowInvalidQuantityException_whenAmountIsNegative() {
        Money negativeAmount = new Money(BigDecimal.valueOf(-50), USD);
        assertThrows(InvalidQuantityException.class, () ->
            new Fee(VALID_TYPE, negativeAmount, VALID_DESCRIPTION, VALID_TIME)
        );
    }
}
