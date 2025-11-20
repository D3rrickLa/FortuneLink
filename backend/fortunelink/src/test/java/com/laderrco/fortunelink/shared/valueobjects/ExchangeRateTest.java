package com.laderrco.fortunelink.shared.valueobjects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyMismatchException;

public class ExchangeRateTest {
    private ValidatedCurrency USD;
    private ValidatedCurrency CAD;
    private BigDecimal rate;
    private Instant date;
    private int precision = Precision.FOREX.getDecimalPlaces();
    @BeforeEach
    void init() {
        USD = ValidatedCurrency.of("USD");
        CAD = ValidatedCurrency.of("CAD");
        rate = BigDecimal.valueOf(1.37).setScale(precision); // from usd to cad
        date = Instant.now();
    }

    @Nested
    public class ConstructorTests {
        @Test
        public void givenNulls_whenConstructor_throwsExceptions() {
            assertAll(
                () -> assertThrows(NullPointerException.class, () -> new ExchangeRate(null, CAD, rate, date, null)),
                () -> assertThrows(NullPointerException.class, () -> new ExchangeRate(USD, null, rate, date, null)),
                () -> assertThrows(NullPointerException.class, () -> new ExchangeRate(USD, CAD, null, date, null)),
                () -> assertThrows(NullPointerException.class, () -> new ExchangeRate(USD, CAD, rate, null, null))
            );
        }

        @Test
        public void givenNegativeRate_whenConstructor_throwsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new ExchangeRate(USD, USD, BigDecimal.valueOf(-0.37), date, null));
            assertEquals("Exchange rate must be positive.", exception.getLocalizedMessage());
        }

        @Test
        public void givenValid_whenConstructor_returnExchangeRate() {
            ExchangeRate usdToCad = new ExchangeRate(USD, CAD, rate, date, null);
            assertAll(
                () -> assertEquals(USD, usdToCad.from()),
                () -> assertEquals(CAD, usdToCad.to()),
                () -> assertEquals(rate, usdToCad.rate()),
                () -> assertEquals(date, usdToCad.exchangeRateDate())
            );
        } 
        
        @Test
        public void givenValid_whenCreate_returnExchangeRate() {
            ExchangeRate usdToCad = ExchangeRate.create(USD, CAD, 1.37, date, null);
            assertAll(
                () -> assertEquals(USD, usdToCad.from()),
                () -> assertEquals(CAD, usdToCad.to()),
                () -> assertEquals(rate, usdToCad.rate()),
                () -> assertEquals(date, usdToCad.exchangeRateDate())
            );
        }

        @Test
        public void givenSmallRate_whenConstructor_returnExchangeRate() {
            ExchangeRate usdToCad = new ExchangeRate(USD, CAD, BigDecimal.valueOf(1.37), date, null);
            assertTrue(usdToCad.rate().scale() == precision);
        }
    }

    @Nested
    public class ConvertInvertTests {
        private ExchangeRate exchangeRate;
        
        @BeforeEach
        void init() {
            exchangeRate = new ExchangeRate(USD, CAD, rate, date, null);
        }

        @Test
        public void givenNull_whenConvert_throwExceptions() {
            assertThrows(NullPointerException.class, () -> exchangeRate.convert(null));
        }

        @Test
        public void givenWrongCurrency_whenConvertTo_throwException() {
            Money eurMoney = Money.of(100, "EUR");
            CurrencyMismatchException exception = assertThrows(CurrencyMismatchException.class, () -> exchangeRate.convert(eurMoney));
            assertEquals("Currency provided does not match `from`", exception.getMessage());
        }
        
        @Test
        public void givenValid_whenConvertTo_returnMoney() {
            Money usdMoney = Money.of(100, "USD");
            Money cadMoneyConverted = exchangeRate.convert(usdMoney);
            Money expectedMoney = Money.of(137, "CAD");
            assertEquals(expectedMoney, cadMoneyConverted);
        }

        @Test
        public void givenNull_whenConvertBack_throwExceptions() {
            assertThrows(NullPointerException.class, () -> exchangeRate.invert(null));
        }

        @Test
        public void givenWrongCurrency_whenConvertBack_throwException() {
            Money eurMoney = Money.of(100, "EUR");
            CurrencyMismatchException exception = assertThrows(CurrencyMismatchException.class, () -> exchangeRate.invert(eurMoney));
            assertEquals("Currency provided does not match `to`", exception.getMessage());
        }
        
        @Test
        public void givenValid_whenConvertBack_returnMoney() {
            Money cadMoney = Money.of(137, "CAD");
            Money usdMoneyConverted = exchangeRate.invert(cadMoney);
            Money expectedMoney = Money.of(100, "USD");
            assertEquals(expectedMoney, usdMoneyConverted);
        }

        @Test
        public void givenSameCurrency_whenConvert_returnSameExchangeRate() {
            exchangeRate = new ExchangeRate(USD, USD, BigDecimal.ONE, date, null);  
            
            Money usdMoney = Money.of(100, "USD");
            Money convertedSameUSD = exchangeRate.convert(usdMoney);
            assertEquals(usdMoney, convertedSameUSD);
        }

        @Test
        public void givenSameCurrency_whenInvert_returnSameExchangeRate() {
            exchangeRate = new ExchangeRate(USD, USD, BigDecimal.ONE, date, null);  
            
            Money usdMoney = Money.of(100, "USD");
            Money convertedSameUSD = exchangeRate.invert(usdMoney);
            assertEquals(usdMoney, convertedSameUSD);
        }

        @Test
        public void givenDifferentCurrency_whenInvertHasWrongExchangeRate_returnException() {
            exchangeRate = new ExchangeRate(USD, CAD, BigDecimal.ONE, date, null);  
            
            Money usdMoney = Money.of(100, "USD");
            assertThrows(CurrencyMismatchException.class, ()->exchangeRate.invert(usdMoney));
           
        }

        @Test
        public void givenDifferentCurrency_whenConvertHasWrongExchangeRate_returnException() {
            exchangeRate = new ExchangeRate(USD, CAD, rate, date, null);  
            
            Money usdMoney = Money.of(100, "CAD");
            assertThrows(CurrencyMismatchException.class, ()->exchangeRate.convert(usdMoney));
           
        }
    }
}
