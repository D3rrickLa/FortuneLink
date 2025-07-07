package com.laderrco.fortunelink.shared.valueobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

public class PercentageTest {

    @Test
    void testConstructorValidInfo() {
        BigDecimal decimal = new BigDecimal(0.024).setScale(6, RoundingMode.HALF_UP);
        Percentage percentage2 = new Percentage(decimal);
        assertNotNull(percentage2);
    }
    
    @Test 
    void testConstructorLessPrecision() {
        BigDecimal decimal = new BigDecimal(0.024).setScale(3, RoundingMode.HALF_UP);
        Percentage percentage2 = new Percentage(decimal);
        assertEquals(decimal.setScale(6, RoundingMode.HALF_UP), percentage2.percentageValue()); 
        
    }
    
    @Test 
    void testConstructorInvalidLessThanZero() {
        BigDecimal decimal = new BigDecimal(-0.02).setScale(6, RoundingMode.HALF_UP);
        assertThrows(IllegalArgumentException.class, ()->new Percentage(decimal));
    }

    @Test
    void testFromDecimal() {
        BigDecimal decimal = new BigDecimal(0.024).setScale(6, RoundingMode.HALF_UP);
        Percentage percentage2 = Percentage.fromDecimal(decimal);
        assertEquals(decimal, percentage2.percentageValue()); 
    }
    
    @Test
    void testFromPercentage() {
        BigDecimal percentage = new BigDecimal(4.45).setScale(6, RoundingMode.HALF_UP);
        Percentage percentage2 = Percentage.fromPercentage(percentage);
        assertEquals(percentage.divide(new BigDecimal(100)), percentage2.percentageValue()); 
        
    }

    @Test
    void testToDecimal() {
        Percentage percentage = new Percentage(new BigDecimal(0.042));
        BigDecimal decimal = percentage.toDecimal();
        assertEquals(new BigDecimal(0.042).setScale(6, RoundingMode.HALF_UP), decimal);
    }
    
    @Test
    void testToPercentage() {
        Percentage percentage = new Percentage(new BigDecimal(0.042));
        BigDecimal perBigDecimal = percentage.toPercentage();
        assertEquals(new BigDecimal(4.2).setScale(6, RoundingMode.HALF_UP), perBigDecimal);
        
    }

}
