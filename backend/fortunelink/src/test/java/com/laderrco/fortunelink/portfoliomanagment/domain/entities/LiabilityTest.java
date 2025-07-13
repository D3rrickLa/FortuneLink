package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class LiabilityTest {
    UUID liabilityId;
    UUID portfolioId;
    String name;
    String description;
    Money defaultCurrency;
    Currency usd;
    Instant maturityDate;
    Percentage defaultInterestRate;

    
    @BeforeEach
    void init() {
        liabilityId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        name = "Liability name";
        description = "liability description";
        usd = Currency.getInstance("USD");
        defaultCurrency = new Money(2500.87, usd);
        maturityDate = Instant.parse("2030-12-10T10:35:45.00Z");
        defaultInterestRate = Percentage.fromPercentage(new BigDecimal("8.76"));
    }

    @Test 
    void testConstructor() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        assertNotNull(testLiability);
    }

    @Test
    void testConstructorInValidNullForAllParameters() {
        Exception e1 = assertThrowsExactly(NullPointerException.class, () -> new Liability(null, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate));
        assertEquals("Liability id cannot be null.", e1.getMessage());

        Exception e2 = assertThrowsExactly(NullPointerException.class, () -> new Liability(liabilityId, null, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate));
        assertEquals("Portfolio id cannot be null.", e2.getMessage());
        Exception e3 = assertThrowsExactly(NullPointerException.class, () -> new Liability(liabilityId, portfolioId, null, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate));
        assertEquals("Name cannot be null.", e3.getMessage());
        Exception e4 = assertThrowsExactly(NullPointerException.class, () -> new Liability(liabilityId, portfolioId, name, description, null, defaultInterestRate, maturityDate, maturityDate));
        assertEquals("Liability balance cannot be null.", e4.getMessage());
        Exception e5 = assertThrowsExactly(NullPointerException.class, () -> new Liability(liabilityId, portfolioId, name, description, defaultCurrency, null, maturityDate, maturityDate));
        assertEquals("Annual interest rate cannot be null.", e5.getMessage());
        Exception e6 = assertThrowsExactly(NullPointerException.class, () -> new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, null, maturityDate));
        assertEquals("Maturity date cannot be null.", e6.getMessage());
        Exception e7 = assertThrowsExactly(NullPointerException.class, () -> new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, null));
        assertEquals("Last interest accrual date cannot be null.", e7.getMessage());
    }

    @Test 
    void testConstructorInValidCurrentBalanceIsNegativeOrZero() {
        defaultCurrency = new Money(0d, usd);
        Exception e1 = assertThrowsExactly(IllegalArgumentException.class, () -> new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate));
        assertEquals("Initial liability balance cannot be a negative value.", e1.getMessage());
    }
    
    @Test
    void testConstructorInValidNameIsBlank() { 
        name = "\r\r";
        Exception e1 = assertThrowsExactly(IllegalArgumentException.class, () -> new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate));
        assertEquals("Liability name cannot be blank.", e1.getMessage());
    }

    @Test 
    void testGetters() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        assertEquals(liabilityId, testLiability.getLiabilityId());
        assertEquals(portfolioId, testLiability.getPortfolioId());
        assertEquals(name, testLiability.getName());
        assertEquals(description, testLiability.getDescription());
        assertEquals(defaultCurrency, testLiability.getCurrentBalance());
        assertEquals(defaultInterestRate, testLiability.getAnnualInterestRate());
        assertEquals(maturityDate, testLiability.getMaturityDate());
        assertEquals(maturityDate, testLiability.getLastInterestAccrualDate());
        assertEquals(usd, testLiability.getLiabilityCurrencyPreference());
    }

    // these test will always be changing
    @Test
    void testAccureInterest() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Money interestAmount = testLiability.calculateAccuredInterest();
        Money expectedAmount = new Money(0, usd);
        assertEquals(expectedAmount, interestAmount);
        
        // testing with a different lastInterestAccuralDate
        Liability testLiability02 = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, Instant.parse("2025-06-06T10:00:00.00Z"));
        Money interestAmount02 = testLiability02.calculateAccuredInterest();
        Instant currentDate = Instant.now();
        long daysBetween = ChronoUnit.DAYS.between(Instant.parse("2025-06-06T10:00:00.00Z"), currentDate);
        
        Money amount;
        
        if (daysBetween <= 0) {
            amount = Money.ZERO(testLiability.getLiabilityCurrencyPreference());
        }
        else{
            // annual rate / 365 = daily rate
            BigDecimal dailyRate = testLiability.getAnnualInterestRate().toDecimal().divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_EVEN);
            // Interest = Principal x Daily Rate x Number of Days
            BigDecimal interestAmount01 = testLiability.getCurrentBalance().amount().multiply(dailyRate).multiply(BigDecimal.valueOf(daysBetween));
            amount = new Money(interestAmount01, usd);
            
        }
        
        
        
        
        assertEquals(amount, interestAmount02);
    }
    
    @Test
    void testCalculateAccuredInterest() {
        Liability testLiability01 = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, Instant.parse("2025-06-06T10:00:00.00Z"));
        
        Instant currentDate = Instant.now();
        long daysBetween = ChronoUnit.DAYS.between(Instant.parse("2025-06-06T10:00:00.00Z"), currentDate);
        
        Money amount;
        
        if (daysBetween <= 0) {
            amount = Money.ZERO(testLiability01.getLiabilityCurrencyPreference());
        }
        else{
            // annual rate / 365 = daily rate
            BigDecimal dailyRate = testLiability01.getAnnualInterestRate().toDecimal().divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_EVEN);
            // Interest = Principal x Daily Rate x Number of Days
            BigDecimal interestAmount = testLiability01.getCurrentBalance().amount().multiply(dailyRate).multiply(BigDecimal.valueOf(daysBetween));
            amount = new Money(interestAmount, usd);
            
        }
        Money expectedAmount = new Money(testLiability01.getCurrentBalance().add(amount).amount(), usd);
        
        Money interest = testLiability01.accureInterest();
        assertEquals(amount, interest);    

        assertEquals(expectedAmount, testLiability01.getCurrentBalance());
    }

    @Test
    void testIncreaseLiabilityBalance() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Money amountToIncrease = new Money(105.45, usd);
        Money expectedMoney = new Money(2606.32d, usd);
        testLiability.increaseLiabilityBalance(amountToIncrease);
        assertEquals(expectedMoney.amount(), testLiability.getCurrentBalance().amount());
    }
    
    @Test 
    void testIncreaseLiabilityBalanceInValidNullValue() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Exception e1 = assertThrows(NullPointerException.class, () -> testLiability.increaseLiabilityBalance(null));
        assertEquals("Amount to increase liability balance cannot be null.", e1.getMessage());
    }
    
    @Test
    void testIncreaseLiabilityBalanceInValidNegativeAndZeroMoney() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Money negativeMoney = new Money(-25.65, usd);
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testLiability.increaseLiabilityBalance(negativeMoney));
        assertEquals("Amount to increase liability balance must be a positive number.", e1.getMessage());
        e1 = assertThrows(IllegalArgumentException.class, () -> testLiability.increaseLiabilityBalance(new Money(0d, usd)));
        assertEquals("Amount to increase liability balance must be a positive number.", e1.getMessage());
    }
    
    @Test 
    void testIncreaseLiabilityBalanceInValidWrongCurrency() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Currency cad = Currency.getInstance("CAD");
        Money cadMoney = new Money(20.45, cad);        
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> testLiability.increaseLiabilityBalance(cadMoney));
        assertEquals("Amount to increase liability balance must be the same currency as the liability currency preference.", e1.getMessage());
    }
    
    @Test 
    void testApplyPayment() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Money principalAmount = new Money(25d, usd);
        testLiability.applyPayment(principalAmount);
        assertEquals(new BigDecimal("2475.87"), testLiability.getCurrentBalance().amount());
    }
    
    @Test 
    void testApplyPaymentInValidNullAmount() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        assertThrows(NullPointerException.class,() ->testLiability.applyPayment(null));
    }

    @Test
    void testApplyPaymentInValidNotRightCurrency() {        
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Currency cad = Currency.getInstance("CAD");
        Money principalAmount = new Money(25d, cad);
        Exception e1 = assertThrows(IllegalArgumentException.class,() ->testLiability.applyPayment(principalAmount));
        assertEquals("Payment amount must be in the same currency as liability's currency preference.", e1.getMessage());
    }
    
    @Test
    void testApplyPaymentInValidNegativeValue() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Money principalAmount = new Money(-25.67, usd);
        Exception e1 = assertThrows(IllegalArgumentException.class,() ->testLiability.applyPayment(principalAmount));
        assertEquals("Payment amount must be a positive number.", e1.getMessage());
    }


    @Test 
    void testReversePayment() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Money principalAmount = new Money(25d, usd);
        testLiability.reversePaymentEffect(principalAmount);
        assertEquals(new BigDecimal("2525.87"), testLiability.getCurrentBalance().amount());
    }
    
    @Test 
    void testReversePaymentInValidNullAmount() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        assertThrows(NullPointerException.class,() ->testLiability.reversePaymentEffect(null));
    }

    @Test
    void testReversePaymentInValidNotRightCurrency() {        
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Currency cad = Currency.getInstance("CAD");
        Money principalAmount = new Money(25d, cad);
        Exception e1 = assertThrows(IllegalArgumentException.class,() ->testLiability.reversePaymentEffect(principalAmount));
        assertEquals("Reverse amount must be in the same currency as liability's currency preference.", e1.getMessage());
    }
    
    @Test
    void testReversePaymentInValidNegativeValue() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Money principalAmount = new Money(-25.67, usd);
        Exception e1 = assertThrows(IllegalArgumentException.class,() ->testLiability.reversePaymentEffect(principalAmount));
        assertEquals("Reverse amount must be a positive number.", e1.getMessage());
    }
   

    @Test
    void testSetAnnualInterestRate() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        
        Percentage newAnnualPercentage = new Percentage(new BigDecimal(7.65));
        testLiability.setAnnualInterestRate(newAnnualPercentage);
        assertEquals(newAnnualPercentage, testLiability.getAnnualInterestRate());
        assertEquals(6, testLiability.getAnnualInterestRate().percentageValue().scale());
    }
    
    @Test
    void testSetAnnualInterestRateInValidNull() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Exception e1 = assertThrows(NullPointerException.class, () -> testLiability.setAnnualInterestRate(null));
        assertEquals("New annual interest rate cannot be null.", e1.getMessage());
    }
    
    @Test
    void testSetAnnualInterestRateInValidNegativeValue() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> 
        testLiability.setAnnualInterestRate(new Percentage(new BigDecimal("-21.3412438"))));
        assertEquals("Percentage value cannot be negative", e1.getMessage());
    }

    @Test
    void testSetDescription() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        assertNotNull(testLiability.getDescription());
        
        String updatedDesc = "some new desc";
        testLiability.setDescription(updatedDesc);
        assertEquals(updatedDesc, testLiability.getDescription());
    }
    
    @Test
    void testSetName() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        assertNotNull(testLiability.getName());
        
        String updatedName = "New name";
        testLiability.setName(updatedName);
        assertEquals(updatedName, testLiability.getName());
    }
    
    
    @Test 
    void testSetNameBadInValidNullAndBlank() {
        Liability testLiability = new Liability(liabilityId, portfolioId, name, description, defaultCurrency, defaultInterestRate, maturityDate, maturityDate);
        assertNotNull(testLiability.getName());

        Exception e1 = assertThrows(NullPointerException.class, () -> testLiability.setName(null));
        assertEquals("New liability name cannot be null.", e1.getMessage());
        Exception e2 = assertThrows(IllegalArgumentException.class, () -> testLiability.setName("\r\r\r\n"));
        assertEquals("New liability name cannot be blank.", e2.getMessage());
        
    }

}
