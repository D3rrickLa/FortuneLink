package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.PortfolioCurrency;

public class LiabilityTest {

    @Test
    void testEquals() {
        UUID lUuid = UUID.randomUUID();
        UUID porUuid = UUID.randomUUID();
        Money curBal = new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$"));
        Percentage percentage = new Percentage(new BigDecimal(10));
        Liability l1 = new Liability(lUuid, porUuid, "SOME LIABILITY", "EPIC", curBal, percentage, LocalDate.now());
        Liability l2 = new Liability(lUuid, porUuid, "SOME LIABILITY", "EPIC", curBal, percentage, LocalDate.now());
        Liability l3 = new Liability(UUID.randomUUID(), porUuid, "SOME LIABILITY", "EPIC", curBal, percentage,
                LocalDate.now());

        assertTrue(l1.equals(l1));
        assertTrue(l1.equals(l2));
        assertFalse(l1.equals(l3));
        assertFalse(l1.equals(null));
        assertFalse(l1.equals(""));
    }

    @Test
    void testConstructorBranches() {
        UUID lUuid = UUID.randomUUID();
        UUID porUuid = UUID.randomUUID();
        Money curBal = new Money(new BigDecimal(10), new PortfolioCurrency("USD", "$"));
        Percentage percentage = new Percentage(new BigDecimal(10));
        assertThrows(IllegalArgumentException.class, () -> new Liability(lUuid, porUuid, "SOME LIABILITY", "EPIC",
                new Money(new BigDecimal(-10), new PortfolioCurrency("USD", "$")), percentage, LocalDate.now()));

        Liability l1 = new Liability(lUuid, porUuid, "SOME LIABILITY", "EPIC", curBal, percentage, LocalDate.now());
        assertEquals(l1, l1);

    }

    @Test
    void testHashCode() {
        UUID lUuid = UUID.randomUUID();
        UUID porUuid = UUID.randomUUID();
        Money curBal = new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$"));
        Percentage percentage = new Percentage(new BigDecimal(10));
        Liability l1 = new Liability(lUuid, porUuid, "SOME LIABILITY", "EPIC", curBal, percentage, LocalDate.now());
        Liability l2 = new Liability(lUuid, porUuid, "SOME LIABILITY", "EPIC", curBal, percentage, LocalDate.now());
        Liability l3 = new Liability(UUID.randomUUID(), porUuid, "SOME LIABILITY", "EPIC", curBal, percentage,
                LocalDate.now());
        assertTrue(l1.hashCode() == l2.hashCode());
        assertTrue(l1.hashCode() != l3.hashCode());
    }

    @Test
    void testMakePaymentStandard() {
        UUID lUuid = UUID.randomUUID();
        UUID porUuid = UUID.randomUUID();
        Money curBal = new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$"));
        Percentage percentage = new Percentage(new BigDecimal(10));
        Liability l1 = new Liability(lUuid, porUuid, "SOME LIABILITY", "EPIC", curBal, percentage, LocalDate.now());

        Money payment = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        l1.makePayment(payment);
    }

    @Test
    void testMakePaymentBranches() {
        UUID lUuid = UUID.randomUUID();
        UUID porUuid = UUID.randomUUID();
        Money curBal = new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$"));
        Percentage percentage = new Percentage(new BigDecimal(10));
        Liability l1 = new Liability(lUuid, porUuid, "SOME LIABILITY", "EPIC", curBal, percentage, LocalDate.now());

        Money payment1 = new Money(new BigDecimal(0), new PortfolioCurrency("USD", "$"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> l1.makePayment(payment1));
        assertTrue(exception.getMessage().contains("Payment amount must be positive."));

        // testing if currency for payment doesn't match the liability
        Money payment2 = new Money(new BigDecimal(10), new PortfolioCurrency("EUR", "$"));
        Exception exception2 = assertThrows(IllegalArgumentException.class, () -> l1.makePayment(payment2));
        assertTrue(exception2.getMessage().contains("Payment currency mismatch with liability's balance currency."));

        // testing if the symbol matches
        Money payment3 = new Money(new BigDecimal(10), new PortfolioCurrency("USD", "&"));
        Exception exception3 = assertThrows(IllegalArgumentException.class, () -> l1.makePayment(payment3));
        assertTrue(exception3.getMessage().contains("Payment currency mismatch with liability's balance currency."));

        // testing the overpayment clause
        Money payment4 = new Money(new BigDecimal(100000), new PortfolioCurrency("USD", "$"));
        Exception exception4 = assertThrows(IllegalArgumentException.class, () -> l1.makePayment(payment4));
        assertTrue(exception4.getMessage().contains("Payment amount " + payment4 + " exceeds current liability balance "+ l1.getCurrentBalance() + " for " + "SOME LIABILITY" + "."));
    }

    @Test
    void testGetMethods() {
        UUID lUuid = UUID.randomUUID();
        UUID porUuid = UUID.randomUUID();
        Money curBal = new Money(new BigDecimal(1000), new PortfolioCurrency("USD", "$"));
        Percentage percentage = new Percentage(new BigDecimal(10));
        Liability l1 = new Liability(lUuid, porUuid, "SOME LIABILITY", "EPIC", curBal, percentage, LocalDate.now());
        Instant ca = l1.getCreatedAt();
        Instant ua = l1.getUpdatedAt();
        assertEquals(lUuid, l1.getLiabilityId());
        assertEquals(porUuid, l1.getPortfolioId());
        assertEquals("SOME LIABILITY", l1.getName());
        assertEquals("EPIC", l1.getDescription());
        assertEquals(curBal, l1.getCurrentBalance());
        assertEquals(percentage, l1.getInterestRate());
        assertEquals(LocalDate.now(), l1.getMaturityDate());
        assertEquals(ca, l1.getCreatedAt());
        assertEquals(ua, l1.getUpdatedAt());
    }
}
