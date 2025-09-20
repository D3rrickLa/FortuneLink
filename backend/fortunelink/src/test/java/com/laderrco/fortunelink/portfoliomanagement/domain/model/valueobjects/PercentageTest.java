package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.Precision;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.Rounding;

public class PercentageTest {
    private BigDecimal percentage25;
    private int decimalPlaces = Precision.PERCENTAGE.getDecimalPlaces();
    private RoundingMode mode = Rounding.PERCENTAGE.getMode();

    @Nested
    @DisplayName("Factory and static tests")
    public class FactoryTests {
        @BeforeEach
        void init() {
            percentage25 = BigDecimal.valueOf(0.25);
            percentage25 = percentage25.setScale(decimalPlaces, mode);
        }

        @Test
        void givenValid_whenConstructor_returnPercentage() {
            Percentage actualPercentage = new Percentage(BigDecimal.valueOf(0.25));
            Percentage expectedPercentage = new Percentage(percentage25);
            assertEquals(expectedPercentage.value().scale(), actualPercentage.value().scale());
            assertEquals(expectedPercentage.value(), actualPercentage.value());

        }

        @Test
        void givenNull_whenConstructor_returnException() {
            NullPointerException exception = assertThrows(NullPointerException.class, ()->
                new Percentage(null)
            );
            assertEquals("Value cannot be null", exception.getLocalizedMessage());
        }

        @Test
        void givenBigDecimal_whenFromPercentage_returnValidPercentage() {
            Percentage actualPercentage = Percentage.fromPercentage(BigDecimal.valueOf(25));
            assertEquals(percentage25, actualPercentage.value());
        }

        @Test
        void givenNull_whenFromPercentage_returnNullException() {
            NullPointerException exception = assertThrows(NullPointerException.class, ()->
                Percentage.fromPercentage(null)
            );
            assertEquals("Percent cannot be null", exception.getLocalizedMessage());
            
        }
    
        @Test
        void givenDouble_whenFromPercentage_returnValidPercentage() {
            Percentage actualPercentage = Percentage.fromPercentage(25.0);
            assertEquals(percentage25, actualPercentage.value());
        }

        @Test
        void givenNull_whenFromPercentageDouble_returnNullException() {
            NullPointerException exception = assertThrows(NullPointerException.class, ()->
                Percentage.fromPercentage(null)
            );
            assertEquals("Percent cannot be null", exception.getLocalizedMessage());
            
        }
    
        @Test
        void givenBigDecimal_whenOf_returnValidPercentage() {
            Percentage actualPercentage = Percentage.of(BigDecimal.valueOf(0.5));
            assertEquals(BigDecimal.valueOf(0.5).setScale(decimalPlaces, mode), actualPercentage.value()); 
        }
    
        @Test
        void givenDouble_whenOf_returnValidPercentage() {
            Percentage actualPercentage = Percentage.of(0.25);
            assertEquals(percentage25, actualPercentage.value()); 
        }
    }

    @Nested
    @DisplayName("Help function tests")
    public class HelperFunctionTests {
        @Test
        void testToPercentage() {
            Percentage percentage = Percentage.of(BigDecimal.valueOf(0.25));
            BigDecimal actualPercentage  = percentage.toPercentage();
            BigDecimal expectedPercentage = BigDecimal.valueOf(25).setScale(decimalPlaces, mode);
            assertEquals(expectedPercentage, actualPercentage);
        }
    
        @Test
        void testCompareTo1() {
            Percentage percentage50 = Percentage.of(BigDecimal.valueOf(0.5));
            Percentage percentage10 = Percentage.of(BigDecimal.valueOf(0.1));
            int actualCompareValue = percentage50.compareTo(percentage10);
            assertEquals(1, actualCompareValue);
        }

        @Test
        void testCompareTo2() {
            Percentage percentage50 = Percentage.of(BigDecimal.valueOf(0.5));
            Percentage percentage10 = Percentage.of(BigDecimal.valueOf(0.51));
            int actualCompareValue = percentage50.compareTo(percentage10);
            assertEquals(-1, actualCompareValue);
        }

        @Test
        void testCompareTo3() {
            Percentage percentage50 = Percentage.of(BigDecimal.valueOf(0.5));
            Percentage percentage10 = Percentage.of(BigDecimal.valueOf(0.500));
            int actualCompareValue = percentage50.compareTo(percentage10);
            assertEquals(0, actualCompareValue);
        }
        
    }

}
