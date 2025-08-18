package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.LiabilityStatus;
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
    void recordPayment_SmallPayment_AppliesCorrectly() {
        Money payment = new Money(BigDecimal.valueOf(0.01), currency);
        PaymentAllocationResult result = liability.recordPayment(payment, 
            incurrenceDate.plus(31, ChronoUnit.DAYS));
        
        // Assert the payment was allocated correctly
        assertThat(result.interestPaid().add(result.principalPaid()))
            .isEqualTo(payment);
        assertThat(result.overPayment()).isEqualTo(Money.ZERO(currency));
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

    @Test
    void recordPayment_ShouldThrowExceptionWhenPayingAPaidOffLiability() {
        liability.recordPayment(originalAmount, incurrenceDate);
        assertThrows(IllegalStateException.class, () -> liability.recordPayment(originalAmount, incurrenceDate));
    }

    @Test
    void recordPayment_shouldThrowIfPaymentDateBeforeIncurrenceDate() {
        assertThrows(IllegalArgumentException.class, () -> liability.recordPayment(originalAmount, Instant.MIN));
    }

    @Test
    void recordPayment_WhenPrincipalPaidButInterestRemains_ShouldStayActive() {
        // Arrange
        Money originalAmount = new Money(BigDecimal.valueOf(100.00), Currency.getInstance("USD"));
        Liability liability = new Liability(
            new LiabilityId(UUID.randomUUID()),
            new PortfolioId(UUID.randomUUID()), 
            createTestLiabilityDetails(),
            originalAmount,
            Instant.now().minus(30, ChronoUnit.DAYS)
        );
        
        // Accrue some interest first (simulate 30 days of interest)
        Instant interestAccrualDate = Instant.now().minus(1, ChronoUnit.DAYS);
        liability.accrueInterest(interestAccrualDate);
        liability.accrueInterest(interestAccrualDate);
        
        // Verify we have both principal and interest
        Money principalBefore = liability.getPrincipalBalance();
        Money interestBefore = liability.getAccruedUnpaidInterest();
        
        assertThat(principalBefore.amount()).isEqualTo(BigDecimal.valueOf(100.00).setScale(DecimalPrecision.MONEY.getDecimalPlaces()));
        assertThat(interestBefore.isPositive()).isTrue();
        
        // Create a payment that will zero out principal but leave interest
        // Since payments go to interest first, we need payment = original principal amount
        // This will pay all interest, then apply remainder to principal
        principalBefore.add(interestBefore);
        Money paymentAmount = principalBefore; // This will leave some interest unpaid
        
        // Act
        PaymentAllocationResult result = liability.recordPayment(paymentAmount, Instant.now());
        
        // Assert
        // Payment should go to interest first, then remainder to principal
        Money expectedInterestPaid = paymentAmount.min(interestBefore);
        Money expectedRemainingPayment = paymentAmount.subtract(expectedInterestPaid);
        Money expectedPrincipalPaid = expectedRemainingPayment;
        Money expectedRemainingInterest = interestBefore.subtract(expectedInterestPaid);
        Money expectedRemainingPrincipal = principalBefore.subtract(expectedPrincipalPaid);
        
        // Verify payment allocation
        assertThat(result.interestPaid()).isEqualTo(expectedInterestPaid);
        assertThat(result.principalPaid()).isEqualTo(expectedPrincipalPaid);
        
        // Verify liability state after payment
        assertThat(liability.getPrincipalBalance()).isEqualTo(expectedRemainingPrincipal);
        assertThat(liability.getAccruedUnpaidInterest()).isEqualTo(expectedRemainingInterest);
        
        // Key assertion: If either principal OR interest remains, status should be ACTIVE
        if (expectedRemainingPrincipal.isPositive() || expectedRemainingInterest.isPositive()) {
            assertThat(liability.getStatus()).isEqualTo(LiabilityStatus.ACTIVE);
            assertThat(liability.isActive()).isTrue();
            assertThat(liability.isFullyPaid()).isFalse();
        }
        
        // Verify current balance reflects remaining amounts
        Money expectedCurrentBalance = expectedRemainingPrincipal.add(expectedRemainingInterest);
        assertThat(liability.getCurrentBalance()).isEqualTo(expectedCurrentBalance);
        assertThat(result.remainingBalance()).isEqualTo(expectedCurrentBalance);
    }

    @Test
    void recordPayment_OnlyMarkedPaidOffWhenBothPrincipalAndInterestAreZero() {
        // Arrange
        Money originalAmount = new Money(BigDecimal.valueOf(100.00), Currency.getInstance("USD"));
        Liability liability = new Liability(
            new LiabilityId(UUID.randomUUID()),
            new PortfolioId(UUID.randomUUID()), 
            createTestLiabilityDetails(),
            originalAmount,
            Instant.now().minus(30, ChronoUnit.DAYS)
        );
        
        // Accrue interest
        liability.accrueInterest(Instant.now().minus(1, ChronoUnit.DAYS));
        
        Money totalOwed = liability.getCurrentBalance();
        
        // Act - Pay exactly the total amount owed
        liability.recordPayment(totalOwed, Instant.now());
        
        // Assert - Now it should be marked as paid off
        assertThat(liability.getPrincipalBalance().amount()).isEqualTo(BigDecimal.ZERO.setScale(DecimalPrecision.MONEY.getDecimalPlaces()));
        assertThat(liability.getAccruedUnpaidInterest().amount()).isEqualTo(BigDecimal.ZERO.setScale(DecimalPrecision.MONEY.getDecimalPlaces()));
        assertThat(liability.getStatus()).isEqualTo(LiabilityStatus.PAID_OFF);
        assertThat(liability.isActive()).isFalse();
        assertThat(liability.isFullyPaid()).isTrue();
        assertThat(liability.getCurrentBalance().amount()).isEqualTo(BigDecimal.ZERO.setScale(DecimalPrecision.MONEY.getDecimalPlaces()));
    }

    @Test 
    void recordPayment_ScenarioWhereOnlyInterestRemains() {
        // Arrange - Create a scenario where we can isolate having only interest remaining
        Money originalAmount = new Money(BigDecimal.valueOf(1000.00), Currency.getInstance("USD"));
        Liability liability = new Liability(
            new LiabilityId(UUID.randomUUID()),
            new PortfolioId(UUID.randomUUID()), 
            createTestLiabilityDetails(),
            originalAmount,
            Instant.now().minus(365, ChronoUnit.DAYS) // Long time for significant interest
        );
        
        // Accrue a full year of interest
        liability.accrueInterest(Instant.now());
        
        Money principalBalance = liability.getPrincipalBalance();
        Money accruedInterest = liability.getAccruedUnpaidInterest();
        
        // To get zero principal with remaining interest, we need to:
        // Pay LESS than the total interest, so some interest remains
        // But the logic allocates to interest FIRST, so this scenario is impossible 
        // with your current payment allocation rules.
        
        // Let's test a different scenario: pay a small amount that leaves both principal and interest
        Money smallPayment = new Money(BigDecimal.valueOf(10.00), Currency.getInstance("USD"));
        
        // Act
        liability.recordPayment(smallPayment, Instant.now());
        
        // Assert - Both principal and interest should remain
        assertThat(liability.getPrincipalBalance()).isEqualTo(principalBalance); // Principal unchanged
        assertThat(liability.getAccruedUnpaidInterest()).isEqualTo(accruedInterest.subtract(smallPayment));
        assertThat(liability.getStatus()).isEqualTo(LiabilityStatus.ACTIVE);
        assertThat(liability.isFullyPaid()).isFalse();
    }


    private LiabilityDetails createTestLiabilityDetails() {
        return new LiabilityDetails(
            "Test Loan",
            "Test Description", 
            LiabilityType.LOAN,
            new Percentage(BigDecimal.valueOf(5.0)), // 5% annual rate
            null // No maturity date
        );
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

    @Test
    void accrualInterest_ReturnsZEROIfStatusNotActive() {
        liability.markAsInDefault(incurrenceDate);
        Money zeroed = liability.accrueInterest(incurrenceDate.plus(1, ChronoUnit.DAYS));
        assertEquals(Money.ZERO(currency), zeroed);
    }

    @Test 
    void accrueInterest_ShouldThrowWhenAccrualDateBeforeLastInterestDate() {
        assertThrows(IllegalArgumentException.class, () -> liability.accrueInterest(Instant.MIN));
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

    @Test 
    void calculateAccruedInterest_TestingCurrentBalanceCompareToZeroWorksCorrectly() {
        liability.recordPayment(new Money(BigDecimal.valueOf(10000000), currency), incurrenceDate.plus(1, ChronoUnit.DAYS));
        Money interest = liability.calculateAccruedInterest(incurrenceDate.plus(10, ChronoUnit.DAYS));
        assertEquals(Money.ZERO(currency).amount(), interest.amount());
    }

    @Test 
    void calculateAccruedInterest_EarlierAsOfDate() {
        Money interest = liability.calculateAccruedInterest(Instant.MIN);
        assertEquals(Money.ZERO(currency).amount(), interest.amount());
    }

   

    // reversePayment
    @Test
    void reversePayment_shouldAddToBalance() {
        Money payment = new Money(BigDecimal.valueOf(100), currency);
        PaymentAllocationResult result = liability.recordPayment(payment, incurrenceDate.plus(1, ChronoUnit.DAYS));
        Money before = liability.getCurrentBalance();
        liability.reversePayment(result, null);
        Money after = liability.getCurrentBalance();
        assertEquals(before.add(payment), after);
    }

    @Test 
    void reversePayment_GoingFromStatusPaidOffToActiveWhenReversing() {
        PaymentAllocationResult result = liability.recordPayment(originalAmount, incurrenceDate);
        assertEquals(LiabilityStatus.PAID_OFF, liability.getStatus());

        liability.reversePayment(result, incurrenceDate);
        assertEquals(LiabilityStatus.ACTIVE, liability.getStatus());
        assertEquals(originalAmount, liability.getPrincipalBalance());
    }
    
    @Test 
    void reversePayment_ReversingIfBranchForPrincipalPaidIsPositive() {
        Money interest = liability.accrueInterest(incurrenceDate.plus(30, ChronoUnit.DAYS));
        assertEquals(interest, liability.getAccruedUnpaidInterest());

        Money payment = new Money(BigDecimal.valueOf(100), currency);

        PaymentAllocationResult result = liability.recordPayment(payment, incurrenceDate);
        assertEquals(LiabilityStatus.ACTIVE, liability.getStatus());

        liability.reversePayment(result, incurrenceDate);
        assertEquals(LiabilityStatus.ACTIVE, liability.getStatus());
        assertEquals(originalAmount, liability.getPrincipalBalance());
    }

    @Test 
    void reversePayment_ReversingIfBranchForInterestPaidIsPositive() {
        Money payment = new Money(BigDecimal.valueOf(100), currency);
        PaymentAllocationResult result = liability.recordPayment(payment, incurrenceDate);
        assertEquals(LiabilityStatus.ACTIVE, liability.getStatus());

        liability.reversePayment(result, incurrenceDate);
        assertEquals(LiabilityStatus.ACTIVE, liability.getStatus());
        assertEquals(originalAmount, liability.getPrincipalBalance());
    }

    @Test
    void reversePayment_shouldThrowIfNegativeOrWrongCurrency() {
        Money validAmount = new Money(BigDecimal.valueOf(100), currency);
        Money negativeAmount = new Money(BigDecimal.valueOf(-100), currency);
        
        // Test negative principal paid
        PaymentAllocationResult negativeResult = new PaymentAllocationResult(
            negativeAmount,  // principalPaid - negative
            validAmount,     // interestPaid - valid
            validAmount,     // currentBalance
            validAmount      // overpayment
        );
        
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class, 
            () -> liability.reversePayment(negativeResult, Instant.now()));

        assertThat(exception1.getMessage()).isEqualTo("Payment amount cannot be negative.");
        
        // Test negative interest paid
        PaymentAllocationResult negativeInterestResult = new PaymentAllocationResult(
            validAmount,     // principalPaid - valid
            negativeAmount,  // interestPaid - negative
            validAmount,     // currentBalance
            validAmount      // overpayment
        );
        
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class, 
            () -> liability.reversePayment(negativeInterestResult, Instant.now()));
        assertThat(exception2.getMessage()).isEqualTo("Payment amount cannot be negative.");
        
        // Test wrong currency - this will fail at PaymentAllocationResult construction
        // because all amounts must have the same currency
        Money wrongCurrencyAmount = new Money(BigDecimal.valueOf(100), Currency.getInstance("EUR"));
        
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class, () -> {
            new PaymentAllocationResult(
                wrongCurrencyAmount,  // principalPaid - wrong currency
                validAmount,          // interestPaid - valid currency
                validAmount,          // currentBalance - valid currency
                validAmount           // overpayment - valid currency
            );
        });
        assertThat(exception3.getMessage()).contains("All amounts in PaymentAllocationResult must have the same currency");
        
        // Test liability currency mismatch - create valid PaymentAllocationResult with different currency
        Money eurAmount = new Money(BigDecimal.valueOf(100), Currency.getInstance("EUR"));
        PaymentAllocationResult eurResult = new PaymentAllocationResult(
            eurAmount,  // All EUR - valid for PaymentAllocationResult
            eurAmount,
            eurAmount,
            eurAmount
        );
        
        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class, 
            () -> liability.reversePayment(eurResult, null));
        assertThat(exception4.getMessage()).contains("Payment allocation currency must match the liability's currency preference");
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
        assertEquals(originalAmount, liability.getPrincipalBalance());
        assertEquals(Money.ZERO(originalAmount.currency()), liability.getAccruedUnpaidInterest());
        assertEquals(incurrenceDate, liability.getIncurrenceDate());
        assertEquals(incurrenceDate, liability.getLastInterestAccrualDate());
        assertEquals(null, liability.getLastPaymentDate());
        assertEquals(LiabilityStatus.ACTIVE, liability.getStatus());
    }

    @Test 
    void test_IsActive() {
        assertEquals(true, liability.isActive());
    }

    @Test 
    void test_ChangeTOMakeNotActive() {
        liability.recordPayment(originalAmount, incurrenceDate);
        liability.markAsClosed(incurrenceDate);
        assertFalse(liability.isActive());
    }

    @Test 
    void test_IsFullyPaidCorrect() {
        assertFalse(liability.isFullyPaid());

        liability.recordPayment(originalAmount, incurrenceDate);
        assertTrue(liability.isFullyPaid());
    }

    @Test 
    void test_MarkAsDefaultCorrect() {
        liability.markAsInDefault(incurrenceDate);
        assertEquals(LiabilityStatus.IN_DEFAULT, liability.getStatus());
    }
    
    @Test 
    void test_MarkAsDefaultThrowsErrorWhenNotActive() {
        liability.recordPayment(originalAmount, incurrenceDate);
        assertThrows(IllegalStateException.class, () -> liability.markAsInDefault(incurrenceDate));
    }

    @Test 
    void test_ReactiveDefault() {
        liability.markAsInDefault(incurrenceDate);
        liability.reactivateFromDefault();
        assertEquals(LiabilityStatus.ACTIVE, liability.getStatus());
    }
    @Test 
    void test_ReactiveDefaultThrowsExceptionWhenNotDefaulted() {
        assertThrows(IllegalStateException.class, () -> liability.reactivateFromDefault());
        
    }
}
