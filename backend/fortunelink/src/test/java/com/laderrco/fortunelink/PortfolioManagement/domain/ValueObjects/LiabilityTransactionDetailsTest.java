package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class LiabilityTransactionDetailsTest {
    private UUID liabilityUuid;
    private Money interestPaid;
    private Money principalChange;
    private LiabilityTransactionDetails liabilityTransactionDetails;

    @BeforeEach
    void init() {
        liabilityUuid = UUID.randomUUID();
        interestPaid = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        principalChange = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        liabilityTransactionDetails = new LiabilityTransactionDetails(liabilityUuid, interestPaid, principalChange);
    }

    @Test
    void testConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new LiabilityTransactionDetails(liabilityUuid,
                new Money(new BigDecimal(-1), new PortfolioCurrency(Currency.getInstance("USD"))), principalChange));
        assertThrows(IllegalArgumentException.class, () -> new LiabilityTransactionDetails(liabilityUuid,
                interestPaid, new Money(new BigDecimal(-1), new PortfolioCurrency(Currency.getInstance("USD")))));
    }

    @Test
    void testEquals() {
        UUID liabilityUuid1 = UUID.randomUUID();
        Money interestPaid1 = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        Money principalChange1 = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        LiabilityTransactionDetails liabilityTransactionDetails1 = new LiabilityTransactionDetails(liabilityUuid,
                interestPaid1, principalChange1);

        assertTrue(liabilityTransactionDetails.equals(liabilityTransactionDetails1));
        assertTrue(liabilityTransactionDetails.equals(liabilityTransactionDetails));
        assertFalse(liabilityTransactionDetails.equals(null));
        assertFalse(liabilityTransactionDetails.equals(new Object()));

        assertFalse(liabilityTransactionDetails.equals(new LiabilityTransactionDetails(liabilityUuid1,
                interestPaid, principalChange1)));

        assertFalse(liabilityTransactionDetails.equals(new LiabilityTransactionDetails(liabilityUuid,
                new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("USD"))), principalChange)));

        assertFalse(liabilityTransactionDetails.equals(new LiabilityTransactionDetails(liabilityUuid,
                interestPaid, new Money(new BigDecimal(20), new PortfolioCurrency(Currency.getInstance("USD"))))));
    }

    @Test
    void testGetters() {
        assertEquals(liabilityUuid, liabilityTransactionDetails.getLiabilityId());
        assertEquals(principalChange, liabilityTransactionDetails.getPrincipalChange());
        assertEquals(interestPaid, liabilityTransactionDetails.getInterestPaid());
    }

    @Test
    void testHashCode() {
        UUID liabilityUuid1 = UUID.randomUUID();
        Money interestPaid1 = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        Money principalChange1 = new Money(new BigDecimal(10), new PortfolioCurrency(Currency.getInstance("USD")));
        LiabilityTransactionDetails liabilityTransactionDetails1 = new LiabilityTransactionDetails(liabilityUuid,
                interestPaid1, principalChange1);
        LiabilityTransactionDetails liabilityTransactionDetails2 = new LiabilityTransactionDetails(liabilityUuid1,
                interestPaid1, principalChange1);

        assertTrue(liabilityTransactionDetails1.hashCode() == liabilityTransactionDetails.hashCode());
        assertFalse(liabilityTransactionDetails2.hashCode() == liabilityTransactionDetails.hashCode());
    }
}
