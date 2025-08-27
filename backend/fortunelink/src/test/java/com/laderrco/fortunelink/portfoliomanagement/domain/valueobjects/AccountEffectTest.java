package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;

public class AccountEffectTest {

    Currency USD = Currency.getInstance("USD");
    Currency CAD = Currency.getInstance("CAD");
    Instant now = Instant.now();

    Money usd100 = Money.of(new BigDecimal("100.00"), USD);
    Money usd90 = Money.of(new BigDecimal("90.00"), USD);
    Money usdNeg100 = Money.of(new BigDecimal("-100.00"), USD);

    CurrencyConversion conversion = new CurrencyConversion(USD, CAD, new BigDecimal("1.25"), now);

    @Test
    void constructor_shouldNotThrowWhenGrossIsZero() {
        Money TEN = Money.of(10, USD);
        MonetaryAmount gross = new MonetaryAmount(Money.ZERO(USD), conversion);
        MonetaryAmount net = new MonetaryAmount(TEN, conversion);   
        assertDoesNotThrow(() -> new AccountEffect(gross, net, CashflowType.DEPOSIT, null));
    }

    @Test
    void constructor_shouldThrowIfGrossAndNetAreZero() {
        Money zero = Money.ZERO(USD);
        MonetaryAmount gross = new MonetaryAmount(zero, conversion);
        MonetaryAmount net = new MonetaryAmount(zero, conversion);

        assertThrows(IllegalArgumentException.class, () ->
            new AccountEffect(gross, net, CashflowType.DIVIDEND, Map.of())
        );
    }

    @Test
    void constructor_shouldThrowIfCurrenciesMismatch() {
        Money cadAmount = Money.of(BigDecimal.TEN, CAD);
        CurrencyConversion conversionCAD = new CurrencyConversion(CAD, USD, new BigDecimal("0.75"), now);
        MonetaryAmount gross = new MonetaryAmount(cadAmount, conversionCAD);
        MonetaryAmount net = new MonetaryAmount(usd100, conversion);

        assertThrows(CurrencyMismatchException.class, () ->
            new AccountEffect(gross, net, CashflowType.DIVIDEND, Map.of())
        );
    }

    @Test
    void constructor_shouldThrowIfPortfolioCurrenciesMismatch() {
        CurrencyConversion altConversion = new CurrencyConversion(USD, USD, BigDecimal.ONE, now);
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion);
        MonetaryAmount net = new MonetaryAmount(usd90, altConversion);

        assertThrows(CurrencyMismatchException.class, () ->
            new AccountEffect(gross, net, CashflowType.DIVIDEND, Map.of())
        );
    }

    @Test
    void constructor_shouldThrowIfIncomeAmountsAreNegative() {
        MonetaryAmount gross = new MonetaryAmount(usdNeg100, conversion);
        MonetaryAmount net = new MonetaryAmount(usdNeg100, conversion);

        assertThrows(IllegalArgumentException.class, () ->
            new AccountEffect(gross, net, CashflowType.DIVIDEND, Map.of())
        );
    }

    @Test
    void constructor_shouldThrowIfExpenseAmountsArePositive() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion);
        MonetaryAmount net = new MonetaryAmount(usd90, conversion);

        assertThrows(IllegalArgumentException.class, () ->
            new AccountEffect(gross, net, CashflowType.FEE, Map.of())
        );
    }

    @Test
    void getFeeAmount_shouldReturnCorrectDifference() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion);
        MonetaryAmount net = new MonetaryAmount(usd90, conversion);

        AccountEffect effect = new AccountEffect(gross, net, CashflowType.DIVIDEND, Map.of());
        assertEquals(new BigDecimal("10.00").setScale(DecimalPrecision.getMoneyDecimalPlaces()), effect.getFeeAmount().nativeAmount().amount());
    }

    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenDIVIDENDGrossIsNegative() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion).negate();
        MonetaryAmount net = new MonetaryAmount(usd90, conversion);
        assertThrows(IllegalArgumentException.class, () -> new AccountEffect(gross, net, CashflowType.DIVIDEND, null));
    }
    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenDIVIDENDNetIsNegative() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion);
        MonetaryAmount net = new MonetaryAmount(usd90, conversion).negate();
        assertThrows(IllegalArgumentException.class, () -> new AccountEffect(gross, net, CashflowType.DIVIDEND, null));
    }
    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenWITHDRAWALGrossIsPositive() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion);
        MonetaryAmount net = new MonetaryAmount(usd90, conversion).negate();
        assertThrows(IllegalArgumentException.class, () -> new AccountEffect(gross, net, CashflowType.WITHDRAWAL, null));
    }
    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenWITHDRAWALNetIsPositive() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion).negate();
        MonetaryAmount net = new MonetaryAmount(usd90, conversion);
        assertThrows(IllegalArgumentException.class, () -> new AccountEffect(gross, net, CashflowType.WITHDRAWAL, null));
    }
    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenWITHDRAWALGrossAndNetIsPositive() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion);
        MonetaryAmount net = new MonetaryAmount(usd90, conversion);
        assertThrows(IllegalArgumentException.class, () -> new AccountEffect(gross, net, CashflowType.WITHDRAWAL, null));
    }
    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenWITHDRAWALGrossAndNetIsNegative() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion).negate();
        MonetaryAmount net = new MonetaryAmount(usd90, conversion).negate();
        assertDoesNotThrow(() -> new AccountEffect(gross, net, CashflowType.WITHDRAWAL, null));
    }

    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenDEPOSITGrossIsNegative() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion).negate();
        MonetaryAmount net = new MonetaryAmount(usd90, conversion);
        assertThrows(IllegalArgumentException.class, () -> new AccountEffect(gross, net, CashflowType.DEPOSIT, null));
    }
    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenDEPOSITNetIsNegative() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion);
        MonetaryAmount net = new MonetaryAmount(usd90, conversion).negate();
        assertThrows(IllegalArgumentException.class, () -> new AccountEffect(gross, net, CashflowType.DEPOSIT, null));
    }
    
    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenUNKNOWNGrossAndNetIsZero() {
        MonetaryAmount gross = new MonetaryAmount(Money.ZERO(USD), conversion);
        MonetaryAmount net = new MonetaryAmount(Money.ZERO(USD), conversion);
        assertThrows(IllegalArgumentException.class, () -> new AccountEffect(gross, net, CashflowType.UNKNOWN, null));
    }
 






    @Test
    void validateAmountsForCashflowType_ShouldThrowWhenErrorTypeGiven() {
        MonetaryAmount gross = new MonetaryAmount(usd100, conversion);
        MonetaryAmount net = new MonetaryAmount(usd90, conversion);
        assertThrows(IllegalArgumentException.class, () -> new AccountEffect(gross, net, CashflowType.ERROR, null));
    }
}
