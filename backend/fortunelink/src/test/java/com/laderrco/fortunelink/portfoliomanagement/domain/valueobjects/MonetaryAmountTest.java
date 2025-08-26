package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;

public class MonetaryAmountTest {
    private Money nativeAmountUSD;
    private CurrencyConversion conversion;

    @BeforeEach
    void init() {
        conversion = new CurrencyConversion(Currency.getInstance("USD"), Currency.getInstance("CAD"), BigDecimal.valueOf(1.42));
        nativeAmountUSD = Money.of(25, Currency.getInstance("USD"));
    }

    @Test
    void test_Constructor_Valid() {
        MonetaryAmount amount = new MonetaryAmount(nativeAmountUSD, conversion);
        assertEquals(Money.of(25, "USD"), amount.nativeAmount());
    }

    @Test 
    void test_ZERO() {
        MonetaryAmount amount = MonetaryAmount.ZERO(Currency.getInstance("USD"));
        assertEquals(BigDecimal.ZERO.setScale(DecimalPrecision.MONEY.getDecimalPlaces()), amount.nativeAmount().amount());
        assertEquals(Currency.getInstance("USD"), amount.conversion().toCurrency());
        assertEquals(Currency.getInstance("USD"), amount.conversion().fromCurrency());
    }

    @Test 
    void test_ZERO_Two_Currencies() {
        MonetaryAmount amount = MonetaryAmount.ZERO(Currency.getInstance("USD"), Currency.getInstance("EUR"));
        assertEquals(BigDecimal.ZERO.setScale(DecimalPrecision.MONEY.getDecimalPlaces()), amount.nativeAmount().amount());
        assertEquals(Currency.getInstance("USD"), amount.nativeAmount().currency());
        assertEquals(Currency.getInstance("USD"), amount.conversion().fromCurrency());
        assertEquals(Currency.getInstance("EUR"), amount.conversion().toCurrency());
    }

    @Test
    void test_Add_Valid() {
        MonetaryAmount amount = MonetaryAmount.of(nativeAmountUSD, conversion);
        MonetaryAmount amount2 = MonetaryAmount.of(nativeAmountUSD.multiply(2), conversion);
        MonetaryAmount addedAmount = amount.add(amount2);
        assertEquals(Money.of(75, "USD"), addedAmount.nativeAmount());
        assertEquals(conversion, addedAmount.conversion());
    }

    @Test
    void test_Add_InValidConversion() {

    }

    @Test 
    void test_Add_NegativeAmountShouldThrowException() {

    }

    @Test
    void test_Multiply_Valid() {
        MonetaryAmount amount = MonetaryAmount.of(nativeAmountUSD, conversion);
        MonetaryAmount multipliedAmount = amount.multiply(BigDecimal.valueOf(2));
        assertEquals(MonetaryAmount.of(Money.of(50, "USD"), conversion), multipliedAmount);
    }

    @Test
    void test_Multiply_BoundaryMultipleOfZero() {
        MonetaryAmount amount = MonetaryAmount.of(nativeAmountUSD, conversion);
        MonetaryAmount multipliedAmount = amount.multiply(BigDecimal.valueOf(0));
        assertEquals(MonetaryAmount.of(Money.of(0, "USD"), conversion), multipliedAmount);
    }

    @Test
    void test_GetConversionAmount() {
        MonetaryAmount amount = MonetaryAmount.of(nativeAmountUSD, conversion);
        assertEquals(Money.of(25*1.42, "CAD"), amount.getConversionAmount());
    }
    
}
