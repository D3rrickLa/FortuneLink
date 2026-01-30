package com.laderrco.fortunelink.portfolio_management.application.commands;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class RecordIncomeCommandTest {
    @Test
    void testConstructorExceptionIsDripTrueSharesNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new RecordIncomeCommand(null, null, null, null, null, null, true, BigDecimal.valueOf(-2), null, null));
    }

    @Test
    void testConstructorExceptionIsDripTrueSharesIsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new RecordIncomeCommand(null, null, null, null, null, null, true, BigDecimal.valueOf(0), null, null));
    }

    @Test
    void testConstructorExceptionIsDripTrueSharesIsOne() {
        assertDoesNotThrow(
                () -> new RecordIncomeCommand(null, null, null, null, null, null, true, BigDecimal.valueOf(1), null, null));
    }

    @Test
    void testConstructorExceptionIsDripFalseSharesIsNull() {
        assertDoesNotThrow(() -> new RecordIncomeCommand(null, null, null, null, null, null, false, null, null, null));
    }

    @Test
    void testConstructorExceptionIsDripFalseSharesIsOne() {
        assertDoesNotThrow(
                () -> new RecordIncomeCommand(null, null, null, null, null, null, false, BigDecimal.valueOf(1), null, null));
    }

    @Test
    @DisplayName("Throws when DRIP true and sharesReceived is null")
    void dripTrueSharesNull_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RecordIncomeCommand(
                PortfolioId.randomId(),
                UserId.randomId(),
                AccountId.randomId(),
                AssetId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                true,
                null,
                Instant.now(),
                "notes"));
    }

    @Test
    @DisplayName("Throws when DRIP true and sharesReceived <= 0")
    void dripTrueSharesZeroOrNegative_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RecordIncomeCommand(
                PortfolioId.randomId(),
                UserId.randomId(),
                AccountId.randomId(),
                AssetId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                true,
                BigDecimal.ZERO,
                Instant.now(),
                "notes"));

        assertThrows(IllegalArgumentException.class, () -> new RecordIncomeCommand(
                PortfolioId.randomId(),
                UserId.randomId(),
                AccountId.randomId(),
                AssetId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                true,
                BigDecimal.valueOf(-5),
                Instant.now(),
                "notes"));
    }

    @Test
    @DisplayName("Succeeds when DRIP true and sharesReceived > 0")
    void dripTrueSharesPositive_succeeds() {
        RecordIncomeCommand cmd = new RecordIncomeCommand(
                PortfolioId.randomId(),
                UserId.randomId(),
                AccountId.randomId(),
                AssetId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                true,
                BigDecimal.valueOf(10),
                Instant.now(),
                "notes");

        assertNotNull(cmd);
        assertEquals(BigDecimal.valueOf(10), cmd.sharesReceived());
        assertTrue(cmd.isDrip());
    }

    @Test
    @DisplayName("Succeeds when DRIP false and sharesReceived is null")
    void dripFalseSharesNull_succeeds() {
        RecordIncomeCommand cmd = new RecordIncomeCommand(
                PortfolioId.randomId(),
                UserId.randomId(),
                AccountId.randomId(),
                AssetId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                false,
                null,
                Instant.now(),
                "notes");

        assertNotNull(cmd);
        assertNull(cmd.sharesReceived());
        assertFalse(cmd.isDrip());
    }

    @Test
    @DisplayName("Succeeds when DRIP false and sharesReceived > 0")
    void dripFalseSharesPositive_succeeds() {
        RecordIncomeCommand cmd = new RecordIncomeCommand(
                PortfolioId.randomId(),
                UserId.randomId(),
                AccountId.randomId(),
                AssetId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                false,
                BigDecimal.valueOf(5),
                Instant.now(),
                "notes");

        assertNotNull(cmd);
        assertEquals(BigDecimal.valueOf(5), cmd.sharesReceived());
        assertFalse(cmd.isDrip());
    }

    @Test
    @DisplayName("Succeeds when DRIP false and sharesReceived <= 0")
    void dripFalseSharesZeroOrNegative_succeeds() {
        RecordIncomeCommand cmdZero = new RecordIncomeCommand(
                PortfolioId.randomId(),
                UserId.randomId(),
                AccountId.randomId(),
                AssetId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                false,
                BigDecimal.ZERO,
                Instant.now(),
                "notes");

        RecordIncomeCommand cmdNegative = new RecordIncomeCommand(
                PortfolioId.randomId(),
                UserId.randomId(),
                AccountId.randomId(),
                AssetId.randomId(),
                new Money(BigDecimal.TEN, ValidatedCurrency.USD),
                TransactionType.DIVIDEND,
                false,
                BigDecimal.valueOf(-5),
                Instant.now(),
                "notes");

        assertNotNull(cmdZero);
        assertNotNull(cmdNegative);
        assertEquals(BigDecimal.ZERO, cmdZero.sharesReceived());
        assertEquals(BigDecimal.valueOf(-5), cmdNegative.sharesReceived());
        assertFalse(cmdZero.isDrip());
        assertFalse(cmdNegative.isDrip());
    }

}
