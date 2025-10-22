package com.laderrco.fortunelink.shared.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.shared.enums.Currency;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import com.laderrco.fortunelink.shared.exception.CurrencyMismatchException;

public class MoneyTest {
    private Money testMoneyCAD;
    private Money testMoneyUSD;
    private BigDecimal amount;
    private Currency CAD;
    private Currency USD;

    @BeforeEach
    void initialSetup() {
        CAD = Currency.CAD;
        USD = Currency.USD;
        amount = BigDecimal.valueOf(100);
        testMoneyCAD = new Money(amount, CAD);
        testMoneyUSD = new Money(amount, USD);
    }

    @Nested
    @DisplayName("factory and constructor methods")
    public class ConstructorAndFactoryTests {
        @Test
        @DisplayName("Testing full args constructor")
        public void givenNone_whenConstructorIsValid_thenReturnNewMoney() {
            Money testMoney = new Money(amount, CAD);
            int expectedDecimalPlaces = Precision.getMoneyPrecision();
            BigDecimal expectedAmount = amount.setScale(expectedDecimalPlaces, RoundingMode.HALF_EVEN);

            assertEquals(expectedAmount, testMoney.amount());
            assertEquals(expectedDecimalPlaces, testMoney.amount().scale());
            assertEquals(CAD, testMoney.currency());
        }
        
        @Test
        @DisplayName("Testing full args constructor, throws error when amount is null")
        public void givenNullInAmount_whenConstructor_thenThrowNullPointerException() {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                new Money(null, CAD)
            );
    
            assertTrue(exception.getLocalizedMessage().equals("amount cannot be null"));
        }

        @Test
        @DisplayName("Testing full args constructor, throws error when currency is null")
        public void givenNullInCurrency_whenConstructor_thenThrowNullPointerException() {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                new Money(amount, null)
            );
    
            assertTrue(exception.getLocalizedMessage().equals("currency cannot be null"));
        }

        @Test 
        @DisplayName("Passes with Of method")
        public void givenValidParemeters_whenOf_thenReturnMoney() {
            Money testMoney = Money.of(amount, CAD);
            int expectedDecimalPlaces = Precision.getMoneyPrecision();
            BigDecimal expectedAmount = amount.setScale(expectedDecimalPlaces, RoundingMode.HALF_EVEN);

            assertEquals(expectedAmount, testMoney.amount());
            assertEquals(expectedDecimalPlaces, testMoney.amount().scale());
            assertEquals(CAD, testMoney.currency());
        }

        @Test 
        @DisplayName("Passes with Of method")
        public void givenValidParemeters_whenOfAlternativeOne_thenReturnMoney() {
            Money testMoney = Money.of(100, "CAD");
            int expectedDecimalPlaces = Precision.getMoneyPrecision();
            BigDecimal expectedAmount = amount.setScale(expectedDecimalPlaces, RoundingMode.HALF_EVEN);

            assertEquals(expectedAmount, testMoney.amount());
            assertEquals(expectedDecimalPlaces, testMoney.amount().scale());
            assertEquals(CAD, testMoney.currency());
        }

        @Test
        @DisplayName("Passes with ZERO method")
        public void givenValidParemeters_whenZero_thenReturnMoney() {
            Money testMoney = Money.ZERO(CAD);
            int expectedDecimalPlaces = Precision.getMoneyPrecision();
            BigDecimal expectedAmount = BigDecimal.ZERO.setScale(expectedDecimalPlaces, RoundingMode.HALF_EVEN);

            assertEquals(expectedAmount, testMoney.amount());
            assertEquals(expectedDecimalPlaces, testMoney.amount().scale());
            assertEquals(CAD, testMoney.currency());
        }
        @Test
        @DisplayName("Passes with ZERO method")
        public void givenValidParemeters_whenZeroAlternativeOne_thenReturnMoney() {
            Money testMoney = Money.ZERO("CAD");
            int expectedDecimalPlaces = Precision.getMoneyPrecision();
            BigDecimal expectedAmount = BigDecimal.ZERO.setScale(expectedDecimalPlaces, RoundingMode.HALF_EVEN);

            assertEquals(expectedAmount, testMoney.amount());
            assertEquals(expectedDecimalPlaces, testMoney.amount().scale());
            assertEquals(CAD, testMoney.currency());
        }
        
    }

    @Nested
    @DisplayName("add() method tests")
    class AddTests{
        @Test
        @DisplayName("givenMoney_whenAdd_thenReturnSummedMoney")
        public void givenMoney_whenAdd_thenReturnSummedMoney() {
            Money actualSummedMoneyCAD = testMoneyCAD.add(testMoneyCAD); 
            Money expectedSummedMoney = new Money(amount.multiply(BigDecimal.valueOf(2)), CAD);
            assertEquals(expectedSummedMoney, actualSummedMoneyCAD);
        }
    
        @Test
        @DisplayName("givenZero_whenAdd_thenReturnsSameAmount")
        public void givenZero_whenAdd_thenReturnsSameAmount() {
            Money actualSummedMoneyCAD = testMoneyCAD.add(new Money(BigDecimal.ZERO, CAD)); 
            Money expectedSummedMoney = new Money((BigDecimal.valueOf(100)), CAD);
            assertEquals(expectedSummedMoney, actualSummedMoneyCAD);
        }
    
        @Test 
        @DisplayName("givenNegativeValue_whenAdd_thenReturnsCorrectAmount")
        public void givenNegativeValue_whenAdd_thenReturnsCorrectAmount() {
            Money actualSummedMoneyCAD = testMoneyCAD.add(new Money(BigDecimal.valueOf(-25), CAD)); 
            Money expectedSummedMoney = new Money((BigDecimal.valueOf(75)), CAD);
            assertEquals(expectedSummedMoney, actualSummedMoneyCAD);
        }
    
        @Test
        @DisplayName("givenDifferentCurrency_whenAdd_thenThrowsCurrencyException")
        public void givenDifferentCurrency_whenAdd_thenThrowsCurrencyException() {
            CurrencyMismatchException cmException = assertThrows(CurrencyMismatchException.class, () -> 
                testMoneyCAD.add(testMoneyUSD)
            );
    
            assertTrue(cmException.getLocalizedMessage().equals("Cannot add different currencies"));
        }
    
        @Test 
        @DisplayName("givenNulls_whenAdd_thenThrowsNullPointerException")
        public void givenNulls_whenAdd_thenThrowsNullPointerException() {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                testMoneyCAD.add(null)
            );
    
            assertTrue(exception.getLocalizedMessage().equals("Cannot add null money"));
        }
    }

    @Nested
    @DisplayName("subtract() method tests")
    class SubtractTests {

        @Test
        @DisplayName("givenMoney_whenSubtract_thenReturnSummedMoney")
        public void givenMoney_whenSubtract_thenReturnSummedMoney() {
            Money actualSummedMoneyCAD = testMoneyCAD.subtract(testMoneyCAD); 
            Money expectedSummedMoney = new Money(amount.multiply(BigDecimal.valueOf(0)), CAD);
            assertEquals(expectedSummedMoney, actualSummedMoneyCAD);
        }
    
        @Test
        @DisplayName("givenZero_whenSubtract_thenReturnsSameAmount")
        public void givenZero_whenSubtract_thenReturnsSameAmount() {
            Money actualSummedMoneyCAD = testMoneyCAD.subtract(new Money(BigDecimal.ZERO, CAD)); 
            Money expectedSummedMoney = new Money((BigDecimal.valueOf(100)), CAD);
            assertEquals(expectedSummedMoney, actualSummedMoneyCAD);
        }
    
        @Test 
        @DisplayName("givenNegativeValue_whenSubtract_thenReturnsCorrectAmount")
        public void givenNegativeValue_whenSubtract_thenReturnsCorrectAmount() {
            Money actualSummedMoneyCAD = testMoneyCAD.subtract(new Money(BigDecimal.valueOf(-25), CAD)); 
            Money expectedSummedMoney = new Money((BigDecimal.valueOf(125)), CAD);
            assertEquals(expectedSummedMoney, actualSummedMoneyCAD);
        }
    
        @Test
        @DisplayName("givenDifferentCurrency_whenSubtract_thenThrowsCurrencyException")
        public void givenDifferentCurrency_whenSubtract_thenThrowsCurrencyException() {
            CurrencyMismatchException cmException = assertThrows(CurrencyMismatchException.class, () -> 
                testMoneyCAD.subtract(testMoneyUSD)
            );
    
            assertTrue(cmException.getLocalizedMessage().equals("Cannot subtract different currencies"));
        }
    
        @Test 
        @DisplayName("givenNulls_whenSubtract_thenThrowsNullPointerException")
        public void givenNulls_whenSubtract_thenThrowsNullPointerException() {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                testMoneyCAD.subtract(null)
            );
    
            assertTrue(exception.getLocalizedMessage().equals("Cannot subtract null money"));
        }
    }

    @Nested
    @DisplayName("multiply() method tests")
    class MultiplyTests {
        @Test
        @DisplayName("givenMoney_whenMultiply_thenReturnProductMoney")
        public void givenMoney_whenMultiply_thenReturnProductMoney() {
            Money actualProductMoneyCAD = testMoneyCAD.multiply(testMoneyCAD.amount()); 
            Money expectedProductMoney = new Money(BigDecimal.valueOf(10000), CAD);
            assertEquals(expectedProductMoney, actualProductMoneyCAD);
        }
    
        @ParameterizedTest
        @ValueSource(doubles = {0.00, 0.000, 0}) 
        @DisplayName("givenZero_whenMultiply_thenReturnsZeroAmount")
        public void givenZero_whenMultiply_thenReturnsZeroAmount(double zeros) {
            Money actualProductMoneyCAD = testMoneyCAD.multiply(BigDecimal.valueOf(zeros)); 
            Money expectedProductMoney = new Money((BigDecimal.valueOf(0.0)), CAD);
            assertEquals(expectedProductMoney, actualProductMoneyCAD);
        }
    
        @Test
        @DisplayName("givenNegativeValue_whenMultiply_thenReturnsCorrectAmount")
        public void givenNegativeValue_whenMultiply_thenReturnsCorrectAmount() {
            Money actualProductMoneyCAD = testMoneyCAD.multiply(BigDecimal.valueOf(-25)); 
            Money expectedProductMoney = new Money((BigDecimal.valueOf(-2500)), CAD);
            assertEquals(expectedProductMoney, actualProductMoneyCAD);
        }

        @Test
        @DisplayName("Boundary test, givenMaxDoubleValue_whenMultiply_thenRetunCorrectAmount")
        public void givenMaxDoubleValue_whenMultiply_thenRetunCorrectAmount() {
            Money actualProductMoneyCAD = testMoneyCAD.multiply(BigDecimal.valueOf(Double.MAX_VALUE)); 
            Money expectedProductMoney = new Money((BigDecimal.valueOf(Double.MAX_VALUE).multiply(amount)), CAD);
            assertEquals(expectedProductMoney, actualProductMoneyCAD);

        }
    
        @Test 
        @DisplayName("givenNulls_whenMultiply_thenThrowsNullPointerException")
        public void givenNulls_whenMultiply_thenThrowsNullPointerException() {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                testMoneyCAD.multiply(null)
            );
    
            assertTrue(exception.getLocalizedMessage().equals("multiply cannot be null"));
        }   
    }

    @Nested
    @DisplayName("divide() method tests")
    class DivideTests {
        @Test
        @DisplayName("givenMoney_whenDivide_thenReturnDividedMoney")
        public void givenMoney_whenDivide_thenReturnDividedMoney() {
            Money actualDividedMoneyCAD = testMoneyCAD.divide(amount);
            Money expectedDividedMoneyCAD = new Money(BigDecimal.ONE, CAD);
            assertEquals(expectedDividedMoneyCAD, actualDividedMoneyCAD);
        }

        @Test 
        @DisplayName("Throws Arthimetic Exception when dividing by zero")
        public void givenZero_whenDivide_thenThrowArthimeticException() {
            ArithmeticException aException = assertThrows(ArithmeticException.class, () ->  
                testMoneyCAD.divide(BigDecimal.ZERO)
            );
            assertTrue(aException.getLocalizedMessage().equals("Cannot divide by zero"));
        } 
        @Test
        @DisplayName("givenNegativeValue_whenDivide_thenReturnsCorrectAmount")
        public void givenNegativeValue_whenDivide_thenReturnsCorrectAmount() {
            Money actualProductMoneyCAD = testMoneyCAD.divide(BigDecimal.valueOf(-25)); 
            Money expectedProductMoney = new Money((BigDecimal.valueOf(-4)), CAD);
            assertEquals(expectedProductMoney, actualProductMoneyCAD);
        }

        @Test
        @DisplayName("Boundary test, givenMaxDoubleValue_whenDivide_thenRetunCorrectAmount")
        public void givenMaxDoubleValue_whenDivide_thenRetunCorrectAmount() {
            Money actualProductMoneyCAD = testMoneyCAD.divide(BigDecimal.valueOf(Double.MAX_VALUE)); 
            Money expectedProductMoney = new Money((amount.divide(BigDecimal.valueOf(Double.MAX_VALUE), Precision.getMoneyPrecision(), Rounding.MONEY.getMode())), CAD);
            assertEquals(expectedProductMoney, actualProductMoneyCAD);

        }
    
        @Test 
        @DisplayName("givenNulls_whenDivide_thenThrowsNullPointerException")
        public void givenNulls_whenDivide_thenThrowsNullPointerException() {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                testMoneyCAD.divide((BigDecimal)null)
            );
    
            assertTrue(exception.getLocalizedMessage().equals("divide cannot be null"));
        }   

        @Test
        @DisplayName("givenQuantity_whenDivide_thenReturnValue")
        public void givenQuantity_whenDivide_thenReturnValue() {
            Quantity quantity = new Quantity(amount);
            Money actualDividedMoneyCAD = testMoneyCAD.divide(quantity);
            Money expectedDividedMoneyCAD = new Money(BigDecimal.ONE, CAD);
            assertEquals(expectedDividedMoneyCAD, actualDividedMoneyCAD);
        }
    }

    @Nested
    @DisplayName("conversion of money tests")
    public class ConversionTest {
        @Test
        public void givenCorrectExchangeRate_whenConvert_returnConvertedAmount() {
            Money testUSDMoney = Money.of(100, "USD");
            ExchangeRate usdToCad = ExchangeRate.create(USD, CAD, 1.34, Instant.now());
            Money acutalConvertedAmount = testUSDMoney.convert(usdToCad);
            Money expectedAmount = Money.of(134, "CAD");
            assertEquals(expectedAmount, acutalConvertedAmount);
        }
        @Test
        public void givenWrongExchangeRateToValue_whenConvert_throwsError() {
            Money testUSDMoney = Money.of(100, "USD");
            ExchangeRate usdToEur = ExchangeRate.create(Currency.EUR, USD, 1.96, Instant.now());
            assertThrows(CurrencyMismatchException.class, () -> testUSDMoney.convert(usdToEur));

        }
        @Test
        public void givenNull_whenConvert_throwsError() {
            Money testUSDMoney = Money.of(100, "USD");
            assertThrows(NullPointerException.class, () -> testUSDMoney.convert(null));

        }
    }

    @Nested
    @DisplayName("logic/other method tests")
    public class LogicTests {
        @Test
        public void givenValidMoney_whenNegate_returnPositiveAmount() {
            Money actualMoney = testMoneyCAD.negate();
            Money expectedMoney = Money.of(Double.parseDouble("-100"), "CAD");
            assertEquals(expectedMoney, actualMoney);
        } 

        @Test
        public void givenValidMoney_whenABS_returnPositiveAmount() {
            Money actualMoney = testMoneyCAD.abs();
            Money expectedMoney = Money.of(Double.parseDouble("100"), "CAD");
            assertEquals(expectedMoney, actualMoney);
        }
        
        @ParameterizedTest
        @ValueSource(doubles = {1, 2, 3, 4, 20, 100, 202, 19.99})
        public void givenPositiveValues_whenIsPositive_returnTrue(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertTrue(money.isPositive());
        }
        @ParameterizedTest
        @ValueSource(doubles = {0, -1, -2, -3, -4, -20, -100, -202, -19.99})
        public void givenNegativeValues_whenIsPositive_returnFalse(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertFalse(money.isPositive());
        }
        @ParameterizedTest
        @ValueSource(doubles = {-1, -2, -3, -4, -20, -100, -202, -19.99})
        public void givenNegativeValues_whenIsNegative_returnTrue(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertTrue(money.isNegative());
        }
        @ParameterizedTest
        @ValueSource(doubles = {0, 1, 2, 3, 4, 20, 100, 202, 19.99})
        public void givenPositiveValues_whenIsNegative_returnFalse(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertFalse(money.isNegative());
        }

        @ParameterizedTest
        @ValueSource(doubles = {0, 0.00, -0.00, 0.000000, 0})
        public void givenDifferentZeroValues_whenIsZero_returnTrue(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertTrue(money.isZero());
        }

        @ParameterizedTest
        @ValueSource(doubles = {-1, -0.01, 0.01, -9.99, Double.MAX_VALUE, Double.MAX_VALUE})
        public void givenDifferentNonZeroValues_whenIsZero_returnFalse(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertFalse(money.isZero());
        }

        @ParameterizedTest
        @ValueSource(doubles = {101, 100.01, 200, 1212, Double.MAX_VALUE})
        public void givenGreaterValue_whenIsGreaterThan_returnFalse(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertFalse(testMoneyCAD.isGreaterThan(money));
        }
        @ParameterizedTest
        @ValueSource(doubles = {87, 99.99, -200, -1212, Double.MIN_VALUE})
        public void givenSmallerValue_whenIsGreaterThan_returnFalse(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertTrue(testMoneyCAD.isGreaterThan(money));
        }

        @Test
        public void givenNullInput_whenIsGreaterThan_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> 
                testMoneyCAD.isLessThan(null)
            );
        }

        @ParameterizedTest
        @ValueSource(doubles = {87, 99.99, -200, -1212, Double.MIN_VALUE})
        public void givenGreaterValue_whenIsLessThan_returnFalse(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertFalse(testMoneyCAD.isLessThan(money));
        }

        @ParameterizedTest
        @ValueSource(doubles = {101, 100.01, 200, 1212, Double.MAX_VALUE})
        public void givenSmallerValue_whenIsLessThan_returnFalse(double value) {
            Money money = new Money(BigDecimal.valueOf(value), CAD);
            assertTrue(testMoneyCAD.isLessThan(money));
        }

        @Test
        public void givenNullInput_whenIsLessThan_thenThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> 
                testMoneyCAD.isLessThan(null)
            );
        }

        @Test
        public void givenValidMoney_whenCompareTo_thenReturnOne() {
            Money money = new Money(BigDecimal.valueOf(99), CAD);
            int actualValue = testMoneyCAD.compareTo(money);
            assertEquals(1, actualValue);

        }
        
        @Test
        public void givenValidMoney_whenCompareTo_thenReturnZERO() {
            Money money = new Money(BigDecimal.valueOf(100), CAD);
            int actualValue = testMoneyCAD.compareTo(money);
            assertEquals(0, actualValue);

        }

        @Test
        public void givenValidMoney_whenCompareTo_thenReturnNegativeOne() {
            Money money = new Money(BigDecimal.valueOf(101), CAD);
            int actualValue = testMoneyCAD.compareTo(money);
            assertEquals(-1, actualValue);

        }

        @Test
        public void givenMoneyDifferentCurrency_whenCompareTo_thenThrowException() {
            Money money = new Money(BigDecimal.valueOf(101), USD);
            assertThrows(CurrencyMismatchException.class, () ->testMoneyCAD.compareTo(money));
        }

        @Test
        public void givenLargerMoney_whenMax_returnLargerAmount() {
            Money expectedAmount = new Money(BigDecimal.valueOf(101), CAD);
            Money actualAmount = testMoneyCAD.max(expectedAmount);
            assertEquals(expectedAmount, actualAmount);
        }

        @Test
        public void givenMoneyWithDifferentCurrency_whenMax_returnException() {
            Money expectedAmount = new Money(BigDecimal.valueOf(101), USD);
            assertThrows(CurrencyMismatchException.class, () ->testMoneyCAD.max(expectedAmount));
        }

        @Test
        public void givenNull_whenMax_returnException() {
            assertThrows(NullPointerException.class, () ->testMoneyCAD.max(null));
        }

        @Test
        public void givenLargerMoney_whenMin_returnLargerAmount() {
            Money expectedAmount = new Money(BigDecimal.valueOf(99), CAD);
            Money actualAmount = testMoneyCAD.min(expectedAmount);
            assertEquals(expectedAmount, actualAmount);
        }

        @Test
        public void givenMoneyWithDifferentCurrency_whenMin_returnException() {
            Money expectedAmount = new Money(BigDecimal.valueOf(101), USD);
            assertThrows(CurrencyMismatchException.class, () ->testMoneyCAD.min(expectedAmount));
        }

        @Test
        public void givenNull_whenMin_returnException() {
            assertThrows(NullPointerException.class, () ->testMoneyCAD.min(null));
        }

    }
}
