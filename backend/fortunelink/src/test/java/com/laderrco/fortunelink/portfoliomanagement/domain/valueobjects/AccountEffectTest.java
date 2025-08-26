package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AccountMetadataKey;
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
    void hasFees_shouldReturnTrueIfFeeExists() {
        AccountEffect effect = new AccountEffect(
            new MonetaryAmount(usd100, conversion),
            new MonetaryAmount(usd90, conversion),
            CashflowType.DIVIDEND,
            Map.of()
        );
        assertTrue(effect.hasFees());
    }

    @Test
    void hasWithholdingTax_shouldDetectMetadataKey() {
        AccountEffect effect = new AccountEffect(
            new MonetaryAmount(usd100, conversion),
            new MonetaryAmount(usd90, conversion),
            CashflowType.DIVIDEND,
            Map.of(AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey(), "0.15")
        );
        assertTrue(effect.hasWithholdingTax());
    }

    @Test
    void getWithholdingTaxRate_shouldParseCorrectly() {
        AccountEffect effect = new AccountEffect(
            new MonetaryAmount(usd100, conversion),
            new MonetaryAmount(usd90, conversion),
            CashflowType.DIVIDEND,
            Map.of(AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey(), "0.15")
        );
        assertEquals(new BigDecimal("0.15"), effect.getWithholdingTaxRate().orElseThrow());
    }

    @Test
    void getWithholdingTaxAmount_shouldCalculateCorrectly() {
        AccountEffect effect = new AccountEffect(
            new MonetaryAmount(usd100, conversion),
            new MonetaryAmount(usd90, conversion),
            CashflowType.DIVIDEND,
            Map.of(AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey(), "0.10")
        );
        assertEquals(new BigDecimal("10.00").setScale(DecimalPrecision.getMoneyDecimalPlaces()), effect.getWitholdingTaxAmount().nativeAmount().amount());
    }

    @Test
    void isValidForCashflowType_shouldReturnTrueForValidIncome() {
        AccountEffect effect = new AccountEffect(
            new MonetaryAmount(usd100, conversion),
            new MonetaryAmount(usd90, conversion),
            CashflowType.DIVIDEND,
            Map.of()
        );
        assertTrue(effect.isValidForCashflowType());
    }

    @Test
    void requiresTaxReporting_shouldReturnTrueForDividend() {
        AccountEffect effect = new AccountEffect(
            new MonetaryAmount(usd100, conversion),
            new MonetaryAmount(usd90, conversion),
            CashflowType.DIVIDEND,
            Map.of()
        );
        assertTrue(effect.requiresTaxReporting());
    }

    @Test
    void isMultiCurrency_shouldReturnTrueIfConversionExists() {
        CurrencyConversion altConversion = new CurrencyConversion(USD, CAD, new BigDecimal("1.25"), now);
        AccountEffect effect = new AccountEffect(
            new MonetaryAmount(usd100, altConversion),
            new MonetaryAmount(usd90, altConversion),
            CashflowType.DIVIDEND,
            Map.of()
        );
        assertTrue(effect.isMultiCurrency());
    }
}
