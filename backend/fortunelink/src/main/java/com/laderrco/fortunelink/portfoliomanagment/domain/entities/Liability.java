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
    private Money principalBalance; // this field is wrong, this is a representation of principal + accured 
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
        this.principalBalance = originalAmount;
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
        
        if (!this.principalBalance.currency().equals(paymentAmount.currency())) {
            throw new IllegalArgumentException("Payment amount must be in the same currency as the liability's currency preference.");             
        }

        Money interestPaid = Money.ZERO(paymentAmount.currency());
        Money principalPaid = Money.ZERO(paymentAmount.currency());
        Money remainingPayment = paymentAmount;

        // apply payment to unpaid interest first
        if (this.accruedUnpaidInterest.isPositive()) {
            Money paymentAppliedToInterest = remainingPayment.min(this.accruedUnpaidInterest);
            interestPaid = paymentAppliedToInterest;
            this.accruedUnpaidInterest = this.accruedUnpaidInterest.subtract(paymentAppliedToInterest);
        }

        // apply payment to principal 
        if (remainingPayment.isPositive()) {
            Money paymentAppliedToPrincipal = remainingPayment.min(this.principalBalance);
            principalPaid = paymentAppliedToPrincipal;

            this.principalBalance = this.principalBalance.subtract(paymentAppliedToPrincipal);
        }

        Money overpayment = Money.ZERO(paymentAmount.currency());
        Money totalOwed = this.principalBalance.add(this.accruedUnpaidInterest);

        if (paymentAmount.compareTo(totalOwed.add(interestPaid).add(principalPaid)) > 0) {
            overpayment = paymentAmount.subtract(interestPaid).subtract(principalPaid);
        }

        return new PaymentAllocationResult(principalPaid, interestPaid, getCurrentBalance(), overpayment);
    }

    public Money accrueInterest(Instant accrualDate) {
        validateParameter(accrualDate, "Accrual date");

        Money newlyAccruedAmount = this.calculateAccruedInterest(accrualDate);
        if (newlyAccruedAmount.amount().compareTo(BigDecimal.ZERO) > 0) {
            // Only update unpaid interest - currentBalance is calculated
            this.accruedUnpaidInterest = this.accruedUnpaidInterest.add(newlyAccruedAmount);
            this.lastInterestAccrualDate = accrualDate;
        }

        return newlyAccruedAmount;
    }
    
    public Money calculateAccruedInterest(Instant asOfDate) {
        validateParameter(asOfDate, "As of date");
        
        if (asOfDate.isBefore(this.lastInterestAccrualDate) || asOfDate.equals(this.lastInterestAccrualDate)) {
            return Money.ZERO(this.principalBalance.currency());            
        }

        long daysBetween = ChronoUnit.DAYS.between(this.lastInterestAccrualDate, asOfDate);

        if (this.principalBalance.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return Money.ZERO(this.principalBalance.currency());
        }

        BigDecimal dailyRate = this.details.annualInterestRate().value()   
            .divide(BigDecimal.valueOf(365), DecimalPrecision.PERCENTAGE.getDecimalPlaces(), RoundingMode.HALF_UP);
        
        BigDecimal interestAmount = this.principalBalance.amount()
            .multiply(dailyRate)
            .multiply(BigDecimal.valueOf(daysBetween));

        return new Money(interestAmount, this.principalBalance.currency());
    }

    public void reversePayment(Money amount) {
        validateParameter(amount, "Reverse amount");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Reverse amount cannot be negative.");
        }

        if (!this.principalBalance.currency().equals(amount.currency())) {
            throw new IllegalArgumentException("Reverse amount must be in the same currency as the liability balance.");
        }

        // Just add it back to principal - simple but potentially inaccurate
        this.principalBalance = this.principalBalance.add(amount);
    }
    
    // should change this to be specific detail's variables
    public void updateDetails(LiabilityDetails details) {
        validateParameter(details, "Liability details");
        this.details = details;
    }
    
    public Money getCurrentBalance() {
        return principalBalance.add(accruedUnpaidInterest);
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

    public Money getPrincipalBalance() {
        return principalBalance;
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
