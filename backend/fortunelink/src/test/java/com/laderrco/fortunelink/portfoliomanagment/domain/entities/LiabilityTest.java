package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.PaymentAllocationResult;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.LiabilityType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;

public class LiabilityTest {
    private Liability liability;
    private final Currency currency = Currency.getInstance("USD");
    private final Money originalAmount = new Money(BigDecimal.valueOf(1000), currency);
    private final Instant incurrenceDate = Instant.parse("2023-01-01T00:00:00Z");
    private LiabilityId liabilityId;
    private PortfolioId portfolioId;
    @BeforeEach
    void setUp() {
        liabilityId = new LiabilityId(UUID.randomUUID());
        portfolioId = new PortfolioId(UUID.randomUUID());
        
        liability = new Liability(
            liabilityId,
            portfolioId,
            new LiabilityDetails("LiabilityName", "LiabilityDescription", LiabilityType.BILLS, Percentage.fromDecimal(BigDecimal.valueOf((0.05))), Instant.MAX), // 5% annual interest
            originalAmount,
            incurrenceDate
        );
    }

    // Constructor
    @Test
    void constructor_shouldThrowIfOriginalAmountIsNegative() {
        Money negativeAmount = new Money(BigDecimal.valueOf(-100), currency);
        assertThrows(IllegalArgumentException.class, () ->
            new Liability(new LiabilityId(UUID.randomUUID()), new PortfolioId(UUID.randomUUID()),
                 new LiabilityDetails("LiabilityName", "LiabilityDescription", LiabilityType.BILLS, Percentage.fromDecimal(BigDecimal.valueOf((0.05))), Instant.now()), negativeAmount, incurrenceDate));
    }

    // recordPayment
    @Test
    void recordPayment_shouldApplyToInterestThenPrincipal() {
        liability.accrueInterest(incurrenceDate.plus(30, ChronoUnit.DAYS));
        Money payment = new Money(BigDecimal.valueOf(100), currency);
        PaymentAllocationResult result = liability.recordPayment(payment, incurrenceDate.plus(31, ChronoUnit.DAYS));

        assertTrue(result.interestPaid().amount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.principalPaid().amount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void recordPayment_shouldThrowIfCurrencyMismatch() {
        Money payment = new Money(BigDecimal.valueOf(100), Currency.getInstance("EUR"));
        assertThrows(IllegalArgumentException.class, () ->
            liability.recordPayment(payment, incurrenceDate.plus(1, ChronoUnit.DAYS)));
    }

    @Test
    void recordPayment_shouldThrowIfPaymentIsZeroOrNegative() {
        Money zero = new Money(BigDecimal.ZERO, currency);
        Money negative = new Money(BigDecimal.valueOf(-10), currency);
        assertThrows(IllegalArgumentException.class, () ->
            liability.recordPayment(zero, incurrenceDate.plus(1, ChronoUnit.DAYS)));
        assertThrows(IllegalArgumentException.class, () ->
            liability.recordPayment(negative, incurrenceDate.plus(1, ChronoUnit.DAYS)));
    }

    @Test
    void recordPayment_shouldHandleOverpayment() {
        Money overpayment = new Money(BigDecimal.valueOf(2000), currency);
        PaymentAllocationResult result = liability.recordPayment(overpayment, incurrenceDate.plus(1, ChronoUnit.DAYS));
        assertEquals(Money.ZERO(currency).amount(), result.remainingBalance().amount());
    }

    // accrueInterest
    @Test
    void accrueInterest_shouldAccrueCorrectly() {
        Money accrued = liability.accrueInterest(incurrenceDate.plus(10, ChronoUnit.DAYS));
        assertTrue(accrued.amount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void accrueInterest_shouldNotAccrueIfDateIsSameOrEarlier() {
        Money accrued = liability.accrueInterest(incurrenceDate);
        assertEquals(Money.ZERO(currency).amount(), accrued.amount());
    }

    // calculateAccruedInterest
    @Test
    void calculateAccruedInterest_shouldReturnZeroIfDateIsSameOrEarlier() {
        Money interest = liability.calculateAccruedInterest(incurrenceDate);
        assertEquals(Money.ZERO(currency).amount(), interest.amount());
    }

    @Test
    void calculateAccruedInterest_shouldHandleNegativePrincipalBoundary() {
        liability.recordPayment(new Money(BigDecimal.valueOf(1000), currency), incurrenceDate.plus(1, ChronoUnit.DAYS));
        Money interest = liability.calculateAccruedInterest(incurrenceDate.plus(10, ChronoUnit.DAYS));
        assertEquals(Money.ZERO(currency).amount(), interest.amount());
    }

    @Test
    void calculateAccruedInterest_shouldHandleNegativePrincipal() {
        liability.recordPayment(new Money(BigDecimal.valueOf(12000), currency), incurrenceDate.plus(1, ChronoUnit.DAYS));
        Money interest = liability.calculateAccruedInterest(incurrenceDate.plus(10, ChronoUnit.DAYS));
        assertEquals(Money.ZERO(currency).amount(), interest.amount());
    }

    // reversePayment
    @Test
    void reversePayment_shouldAddToBalance() {
        Money payment = new Money(BigDecimal.valueOf(100), currency);
        liability.recordPayment(payment, incurrenceDate.plus(1, ChronoUnit.DAYS));
        Money before = liability.getCurrentBalance();
        liability.reversePayment(payment);
        Money after = liability.getCurrentBalance();
        assertEquals(before.add(payment), after);
    }

    @Test
    void reversePayment_shouldThrowIfNegativeOrWrongCurrency() {
        Money negative = new Money(BigDecimal.valueOf(-100), currency);
        Money wrongCurrency = new Money(BigDecimal.valueOf(100), Currency.getInstance("EUR"));
        assertThrows(IllegalArgumentException.class, () -> liability.reversePayment(negative));
        assertThrows(IllegalArgumentException.class, () -> liability.reversePayment(wrongCurrency));
    }

    // updateDetails
    @Test
    void updateDetails_shouldUpdateSuccessfully() {
        LiabilityDetails newDetails = new LiabilityDetails("LiabilityName", "LiabilityDescription", LiabilityType.BILLS, Percentage.fromDecimal(BigDecimal.valueOf(0.1)), Instant.now());
        liability.updateDetails(newDetails);
        assertEquals(newDetails, liability.getDetails());
    }

    @Test
    void updateDetails_shouldThrowIfNull() {
        assertThrows(NullPointerException.class, () -> liability.updateDetails(null));
    }


    @Test 
    void testGetters() {
        assertEquals(currency, liability.getCurrentBalance().currency());
        assertEquals(liabilityId, liability.getLiabilityId());
        assertEquals(portfolioId, liability.getPortfolioId());
        assertEquals(originalAmount, liability.getOriginalAmount());
        assertEquals(Money.ZERO(originalAmount.currency()), liability.getAccruedUnpaidInterest());
        assertEquals(incurrenceDate, liability.getIncurrenceDate());
        assertEquals(incurrenceDate, liability.getLastInterestAccrualDate());
    }
}
