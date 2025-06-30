package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Liability;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Percentage;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class LiabilityTest {
    private Liability liability;
    private UUID porfolioId = UUID.randomUUID();
    private UUID liabilityId = UUID.randomUUID();
    private Money currentBalance;
    private Percentage interestRate;
    private ZonedDateTime matureDateTime;

    @BeforeEach
    void init() {
        currentBalance = new Money(new BigDecimal(10000), new PortfolioCurrency(Currency.getInstance("USD")));
        interestRate = new Percentage(new BigDecimal(0.05D));
        matureDateTime = ZonedDateTime.now();
        liability = new Liability(liabilityId, porfolioId, "Test Liability", "SOME test", currentBalance, interestRate,
                matureDateTime);
    }

    @Test
    void testConstructor() {
        // tests for constructor Objects.requireNonNull snippets
        Exception e1 = assertThrows(NullPointerException.class,
                () -> new Liability(liabilityId, null, "Test Liability", "SOME test", currentBalance,
                        interestRate,
                        matureDateTime));
        assertTrue(e1.getMessage().equals("Portfolio ID cannot be null."));

        Exception e2 = assertThrows(NullPointerException.class,
                () -> new Liability(null, porfolioId, "Test Liability", "SOME test", currentBalance,
                        interestRate,
                        matureDateTime));
        assertTrue(e2.getMessage().equals("Liability ID cannot be null."));

        Exception e3 = assertThrows(NullPointerException.class,
                () -> new Liability(liabilityId, porfolioId, null, "SOME test", currentBalance,
                        interestRate,
                        matureDateTime));
        assertTrue(e3.getMessage().equals("Liability name cannot be null."));

        Exception e4 = assertThrows(NullPointerException.class,
                () -> new Liability(liabilityId, porfolioId, "Test Liability", "SOME test", null,
                        interestRate,
                        matureDateTime));
        assertTrue(e4.getMessage().equals("Current Balance cannot be null."));

        Exception e5 = assertThrows(NullPointerException.class,
                () -> new Liability(liabilityId, porfolioId, "Test Liability", "SOME test", currentBalance,
                        null,
                        matureDateTime));
        assertTrue(e5.getMessage().equals("Interest rate cannot be null."));

        Exception e6 = assertThrows(NullPointerException.class,
                () -> new Liability(liabilityId, porfolioId, "Test Liability", "SOME test", currentBalance,
                        interestRate,
                        null));
        assertTrue(e6.getMessage().equals("Maturity date cannot be null."));

        // testing ot see if balance is less than or equal to 0, should throw error
        Money currentBalanceNegative = new Money(new BigDecimal(-10000),
                new PortfolioCurrency(Currency.getInstance("USD")));
        assertThrows(IllegalArgumentException.class,
                () -> new Liability(liabilityId, porfolioId, "Test Liability", null, currentBalanceNegative,
                        interestRate,
                        matureDateTime));

        assertEquals(liabilityId, liability.getLiabilityId());
        assertEquals(porfolioId, liability.getPortfolioId());
    }

    @Test
    void testChangeLiabilityDescription() {
        String oldDesc = liability.getLiabilitDescription();
        liability.changeLiabilityDescription("SOMETHING NEW");
        assertFalse(liability.getLiabilitDescription().equals(oldDesc));
        assertTrue(liability.getLiabilitDescription().equals("SOMETHING NEW"));
    }

    @Test
    void testChangeLiabilityName() {
        // test to see if method throws exception when passed null
        assertThrows(NullPointerException.class, () -> liability.changeLiabilityName(null));

        // tests to see if method throws exception when passed ""
        Exception e1 = assertThrows(IllegalArgumentException.class, () -> liability.changeLiabilityName(""));
        assertTrue(e1.getMessage().equals("Liability name cannot be empty."));

        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> liability.changeLiabilityName("   "));
        assertTrue(e2.getMessage().equals("Liability name cannot be empty."));

        String oldName = liability.getLiabilityName();
        liability.changeLiabilityName("SOMETHING NEW");
        assertFalse(liability.getLiabilityName().equals(oldName));
        assertTrue(liability.getLiabilityName().equals("SOMETHING NEW"));
    }

    @Test
    void testIncreaseLiabilityBalance() {
        Money negativePayment = new Money(new BigDecimal(-100), new PortfolioCurrency(Currency.getInstance("USD")));
        Money zeroedPayment = new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD")));
        Money diffCurrencyPayment = new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("CAD")));

        // test and see if methods throws error if money is null, updating the balance
        assertThrows(NullPointerException.class, () -> liability.increaseLiabilityBalance(null));

        // tests to see if method throws error for not valid payments
        Exception e1 = assertThrows(IllegalArgumentException.class,
                () -> liability.increaseLiabilityBalance(negativePayment));
        assertTrue(
                e1.getMessage().equals("Amount to increase Liability balance cannot be less than or equal to zero."));

        Exception e2 = assertThrows(IllegalArgumentException.class,
                () -> liability.increaseLiabilityBalance(zeroedPayment));
        assertTrue(
                e2.getMessage().equals("Amount to increase Liability balance cannot be less than or equal to zero."));

        Exception e3 = assertThrows(IllegalArgumentException.class,
                () -> liability.increaseLiabilityBalance(diffCurrencyPayment));
        assertTrue(e3.getMessage().equals("Increase value must be the same currency with the original liability."));

        liability.increaseLiabilityBalance(liability.getCurrentLiabilityBalance());
        assertEquals(new Money(new BigDecimal(20000), new PortfolioCurrency(Currency.getInstance("USD"))),
                liability.getCurrentLiabilityBalance());
    }

    @Test
    void testMakeLiabilityPayment() {
        Money negativePayment = new Money(new BigDecimal(-100), new PortfolioCurrency(Currency.getInstance("USD")));
        Money zeroedPayment = new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD")));
        Money diffCurrencyPayment = new Money(new BigDecimal(100), new PortfolioCurrency(Currency.getInstance("CAD")));
        Money tooMuchPaidPayment = new Money(new BigDecimal(100000),
                new PortfolioCurrency(Currency.getInstance("USD")));

        // test and see if methods throws error if money is null, updating the balance
        assertThrows(NullPointerException.class, () -> liability.makeLiabilityPayment(null));

        // tests to see if method throws error for not valid payments
        Exception e1 = assertThrows(IllegalArgumentException.class,
                () -> liability.makeLiabilityPayment(negativePayment));
        assertTrue(e1.getMessage().equals("Amount to pay off Liability balance cannot be less than or equal to zero."));

        Exception e2 = assertThrows(IllegalArgumentException.class,
                () -> liability.makeLiabilityPayment(zeroedPayment));
        assertTrue(e2.getMessage().equals("Amount to pay off Liability balance cannot be less than or equal to zero."));

        Exception e3 = assertThrows(IllegalArgumentException.class,
                () -> liability.makeLiabilityPayment(diffCurrencyPayment));
        assertTrue(e3.getMessage().equals("Payment amount must be the same currency with the original liability."));

        Exception e4 = assertThrows(IllegalArgumentException.class,
                () -> liability.makeLiabilityPayment(tooMuchPaidPayment));
        assertTrue(e4.getMessage().equals(String.format("%s excees the current liability balance of %s for %s",
                tooMuchPaidPayment, liability.getCurrentLiabilityBalance(), liability.getLiabilityName())));

        liability.makeLiabilityPayment(liability.getCurrentLiabilityBalance());
        assertEquals(new Money(new BigDecimal(0), new PortfolioCurrency(Currency.getInstance("USD"))),
                liability.getCurrentLiabilityBalance());

    }

    @Test
    void testUpdateInterestRate() {

        assertThrows(NullPointerException.class, () -> liability.updateInterestRate(null));

        // Percentage interestNegative = new Percentage(new BigDecimal(0));
        // Exception e1 = assertThrows(IllegalArgumentException.class,
        // () -> liability.updateInterestRate(interestNegative));
        // assertTrue(e1.getMessage().equals("Amount to pay off Liability balance cannot
        // be less than or equal to zero."));

        liability.updateInterestRate(new Percentage(new BigDecimal(0.0342)));
        assertEquals(new BigDecimal(0.0342), liability.getInterestRate().percentValue());

    }

    @Test
    void testUpdateMaturityDate() {
        assertThrows(NullPointerException.class, () -> liability.updateMaturityDate(null));
        ZonedDateTime now = ZonedDateTime.now();
        liability.updateMaturityDate(now);
        assertEquals(now, liability.getMaturityDate());

    }
}
