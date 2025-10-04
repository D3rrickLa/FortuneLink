package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyAreTheSameException;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.Precision;
import com.laderrco.fortunelink.shared.domain.valueobjects.Money;

public class ExchangeRateTest {
    private Currency USD;
    private Currency CAD;
    private BigDecimal rate;
    private Instant date;
    private int precision = Precision.FOREX.getDecimalPlaces();
    @BeforeEach
    void init() {
        USD = Currency.getInstance("USD");
        CAD = Currency.getInstance("CAD");
        rate = BigDecimal.valueOf(1.37).setScale(precision); // from usd to cad
        date = Instant.now();
    }

    @Nested
    public class ConstructorTests {
        @Test
        public void givenNulls_whenConstructor_throwsExceptions() {
            assertAll(
                () -> assertThrows(NullPointerException.class, () -> new ExchangeRate(null, CAD, rate, date)),
                () -> assertThrows(NullPointerException.class, () -> new ExchangeRate(USD, null, rate, date)),
                () -> assertThrows(NullPointerException.class, () -> new ExchangeRate(USD, CAD, null, date)),
                () -> assertThrows(NullPointerException.class, () -> new ExchangeRate(USD, CAD, rate, null))
            );
        }

        @Test
        public void givenSameCurrency_whenConstructor_throwsException() {
            CurrencyAreTheSameException exception = assertThrows(CurrencyAreTheSameException.class, () -> new ExchangeRate(USD, USD, rate, date));
            assertEquals("'fromCurrency' and 'toCurrency' are the same", exception.getLocalizedMessage());
        }

        @Test
        public void givenValid_whenConstructor_returnExchangeRate() {
            ExchangeRate usdToCad = new ExchangeRate(USD, CAD, rate, date);
            assertAll(
                () -> assertEquals(USD, usdToCad.fromCurrency()),
                () -> assertEquals(CAD, usdToCad.toCurrency()),
                () -> assertEquals(rate, usdToCad.rate()),
                () -> assertEquals(date, usdToCad.exchangeDate())
            );
        } 
        
        @Test
        public void givenValid_whenCreate_returnExchangeRate() {
            ExchangeRate usdToCad = ExchangeRate.create("USD", "CAD", 1.37, date);
            assertAll(
                () -> assertEquals(USD, usdToCad.fromCurrency()),
                () -> assertEquals(CAD, usdToCad.toCurrency()),
                () -> assertEquals(rate, usdToCad.rate()),
                () -> assertEquals(date, usdToCad.exchangeDate())
            );
        }
    }

    @Nested
    public class ConvertTests {
        private ExchangeRate exchangeRate;
        
        @BeforeEach
        void init() {
            exchangeRate = new ExchangeRate(USD, CAD, rate, date);
        }

        @Test
        public void givenNull_whenConvertTo_throwExceptions() {
            assertThrows(NullPointerException.class, () -> exchangeRate.convertTo(null));
        }

        @Test
        public void givenWrongCurrency_whenConvertTo_throwException() {
            Money eurMoney = Money.of(100, "EUR");
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> exchangeRate.convertTo(eurMoney));
            assertEquals("Currency provided does not match `fromCurrency`", exception.getMessage());
        }
        
        @Test
        public void givenValid_whenConvertTo_returnMoney() {
            Money usdMoney = Money.of(100, "USD");
            Money cadMoneyConverted = exchangeRate.convertTo(usdMoney);
            Money expectedMoney = Money.of(137, "CAD");
            assertEquals(expectedMoney, cadMoneyConverted);
        }

        @Test
        public void givenNull_whenConvertBack_throwExceptions() {
            assertThrows(NullPointerException.class, () -> exchangeRate.convertBack(null));
        }

        @Test
        public void givenWrongCurrency_whenConvertBack_throwException() {
            Money eurMoney = Money.of(100, "EUR");
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> exchangeRate.convertBack(eurMoney));
            assertEquals("Currency provided does not match `toCurrency`", exception.getMessage());
        }
        
        @Test
        public void givenValid_whenConvertBack_returnMoney() {
            Money cadMoney = Money.of(137, "CAD");
            Money usdMoneyConverted = exchangeRate.convertBack(cadMoney);
            Money expectedMoney = Money.of(100, "USD");
            assertEquals(expectedMoney, usdMoneyConverted);
        }
    }
}
