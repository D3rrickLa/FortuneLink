package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.PaymentAllocationResult;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;

// when doing the networth calculation and or the subtraction of the cash balance, we use the exchange rate service
public class Liability {
    // Unique identity and link to the Aggregate Root
    private final LiabilityId liabilityId;
    private final PortfolioId portfolioId;

    // A Value Object to encapsulate and validate liability details
    private LiabilityDetails details; // allow for direct edits

    // Monetary values stored in the liability's native currency
    private final Money originalAmount;
    private Money currentBalance; // this field is wrong, this is a representation of principal + accured 
    private Money accruedUnpaidInterest;

    // Lifecycle and Audit Fields
    private final Instant incurrenceDate;
    private Instant lastInterestAccrualDate;
   
    public Liability(
        LiabilityId liabilityId,
        PortfolioId portfolioId,
        LiabilityDetails details,
        Money originalAmount,
        Instant incurrenceDate
    ) {
        validateParameter(liabilityId, "Liability id");
        validateParameter(portfolioId, "Portfolio id");
        validateParameter(details, "details");
        validateParameter(originalAmount, "Original amount");
        validateParameter(incurrenceDate, "Incurrence date");

        if (!originalAmount.isPositive()) {
            throw new IllegalArgumentException("Original amount must be positive.");
        }
        this.liabilityId = liabilityId;
        this.portfolioId = portfolioId;
        this.details = details;
        this.originalAmount = originalAmount;
        this.currentBalance = originalAmount;
        this.accruedUnpaidInterest = Money.ZERO(originalAmount.currency());
        this.incurrenceDate = incurrenceDate;
        this.lastInterestAccrualDate = incurrenceDate;
    }

     private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }

    public PaymentAllocationResult recordPayment(Money paymentAmount, Instant paymentDate) {
        validateParameter(paymentAmount, "Payment amount");
        validateParameter(paymentDate, "Payment date");

        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be a positive value.");
        }
        
        if (!this.currentBalance.currency().equals(paymentAmount.currency())) {
            throw new IllegalArgumentException("Payment amount must be in the same currency as the liability's currency preference.");             
        }

        Money interestPaid = Money.ZERO(paymentAmount.currency());
    }

    public Money accrueInterest(Instant accrualDate) {
        validateParameter(accrualDate, "Accural date");

        Money newlyAccruedAmount = this.calculateAccruedInterest(accrualDate);
        if (newlyAccruedAmount.amount().compareTo(BigDecimal.ZERO) > 0) {
            this.currentBalance = this.currentBalance.add(newlyAccruedAmount);

            this.accruedUnpaidInterest = this.accruedUnpaidInterest.add(newlyAccruedAmount);
            this.lastInterestAccrualDate = accrualDate;
        }

        return newlyAccruedAmount;
    }
    
    public Money calculateAccruedInterest(Instant asOfDate) {
        validateParameter(asOfDate, "As of date");
        if (asOfDate.isBefore(this.lastInterestAccrualDate) || asOfDate.equals(this.lastInterestAccrualDate)) {
            return Money.ZERO(this.currentBalance.currency());
        }
        
        long daysBetween = ChronoUnit.DAYS.between(this.lastInterestAccrualDate, asOfDate);
        Money principalForCalculation = this.currentBalance.subtract(this.accruedUnpaidInterest);
        
        // If current balance is negative OR principal portion is negative/zero, no interest should accrue
        // the first part of the if statement might not even run do to how the payment process work
        if (this.currentBalance.amount().compareTo(BigDecimal.ZERO) < 0 || 
            principalForCalculation.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return Money.ZERO(this.currentBalance.currency());
        }
        
        BigDecimal dailyRate = this.details.annualInterestRate().value()
            .divide(BigDecimal.valueOf(365), DecimalPrecision.PERCENTAGE.getDecimalPlaces(), RoundingMode.HALF_UP);

        BigDecimal interestAmount = principalForCalculation.amount()
            .multiply(dailyRate)
            .multiply(BigDecimal.valueOf(daysBetween));
        
        return new Money(interestAmount, this.currentBalance.currency());
    }

    public void reversePayment(Money amount) {
        validateParameter(amount, "Reverse amount");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Reverse amount cannot be negative.");
        }

        if (!this.currentBalance.currency().equals(amount.currency())) {
            throw new IllegalArgumentException("Reverse amount must be in the same currency as the liability balance.");
        }

        this.currentBalance = this.currentBalance.add(amount);
    
    }
    
    // should change this to be specific detail's variables
    public void updateDetails(LiabilityDetails details) {
        validateParameter(details, "Liability details");
        this.details = details;
    }
    
    public LiabilityId getLiabilityId() {
        return liabilityId;
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
    }

    public LiabilityDetails getDetails() {
        return details;
    }

    public Money getOriginalAmount() {
        return originalAmount;
    }

    public Money getCurrentBalance() {
        return currentBalance;
    }

    public Money getAccruedUnpaidInterest() {
        return accruedUnpaidInterest;
    }

    public Instant getIncurrenceDate() {
        return incurrenceDate;
    }

    public Instant getLastInterestAccrualDate() {
        return lastInterestAccrualDate;
    }

}
