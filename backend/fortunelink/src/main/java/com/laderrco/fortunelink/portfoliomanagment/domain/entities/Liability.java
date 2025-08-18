package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.PaymentAllocationResult;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.LiabilityStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;

// when doing the networth calculation and or the subtraction of the cash balance, we use the exchange rate service
/* things it cant do
 * no mini payment calculations
 * no late fees or penalites 
 * no payment due dates
 * no grace periods
 * 
 * 
 * we are doing simple interest, only on principal and not complex (on interest as well and or time weighted - can change interest mid way)
 */
public class Liability {
    // Unique identity and link to the Aggregate Root
    private final LiabilityId liabilityId;
    private final PortfolioId portfolioId;

    // A Value Object to encapsulate and validate liability details
    private LiabilityDetails details; // allow for direct edits

    // Monetary values stored in the liability's native currency
    private final Money originalAmount;
    private Money principalBalance;
    private Money accruedUnpaidInterest;

    // Lifecycle and Audit Fields
    private final Instant incurrenceDate;
    private Instant lastPaymentDate;
    private Instant lastInterestAccrualDate;
    private LiabilityStatus status;
   
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
        this.principalBalance = originalAmount; // This should ONLY be principal
        this.accruedUnpaidInterest = Money.ZERO(originalAmount.currency());
        this.incurrenceDate = incurrenceDate;
        this.lastInterestAccrualDate = incurrenceDate;
        this.lastPaymentDate = null; // No payments made yet
        this.status = LiabilityStatus.ACTIVE;
    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null.", parameterName));
    }
    
    // this technically might not event run due to our previous rules
    private void validateBusinessInvariants() {
        if (principalBalance.isNegative()) {
            throw new IllegalStateException("Principal balance cannot be negative");
        }
        if (accruedUnpaidInterest.isNegative()) {
            throw new IllegalStateException("Accrued interest cannot be negative");
        }
    }
    
    public PaymentAllocationResult recordPayment(Money paymentAmount, Instant paymentDate) {
        validateParameter(paymentAmount, "Payment amount");
        validateParameter(paymentDate, "Payment date");

        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be a positive value.");
        }
        
        if (!paymentAmount.currency().equals(getNativeCurrency())) {
            throw new IllegalArgumentException("Payment amount must be in the same currency as the liability's currency preference.");             
        }        
        
        if (this.status == LiabilityStatus.PAID_OFF) {
            throw new IllegalStateException("Cannot record payment on a paid-off liability.");
        }
        
        if (paymentDate.isBefore(this.incurrenceDate)) {
            throw new IllegalArgumentException("Payment date cannot be before incurrence date.");
        }

        // Calculate total owed and determine overpayment upfront
        Money totalOwed = this.principalBalance.add(this.accruedUnpaidInterest);
        Money effectivePayment = paymentAmount.min(totalOwed); // only allocate what can acutally be applied to the debt
        Money overpayment = paymentAmount.subtract(effectivePayment);

        Money interestPaid = Money.ZERO(paymentAmount.currency());
        Money principalPaid = Money.ZERO(paymentAmount.currency());
        Money remainingPayment = effectivePayment;

        // Apply payment to unpaid interest first
        if (this.accruedUnpaidInterest.isPositive()) {
            Money paymentAppliedToInterest = remainingPayment.min(this.accruedUnpaidInterest);
            interestPaid = paymentAppliedToInterest;
            remainingPayment = remainingPayment.subtract(paymentAppliedToInterest);
            this.accruedUnpaidInterest = this.accruedUnpaidInterest.subtract(paymentAppliedToInterest);
        }

        // Apply remaining payment to principal
        if (remainingPayment.isPositive()) {
            Money paymentAppliedToPrincipal = remainingPayment.min(this.principalBalance);
            principalPaid = paymentAppliedToPrincipal;
            this.principalBalance = this.principalBalance.subtract(paymentAppliedToPrincipal);
        }

        // Update payment tracking
        this.lastPaymentDate = paymentDate;
        
        // Check if liability is now fully paid
        if (this.principalBalance.amount().compareTo(BigDecimal.ZERO) == 0 && 
            this.accruedUnpaidInterest.amount().compareTo(BigDecimal.ZERO) == 0) {
            this.status = LiabilityStatus.PAID_OFF;
        }

        validateBusinessInvariants();
        
        return new PaymentAllocationResult(principalPaid, interestPaid, getCurrentBalance(), overpayment);
    }

    public Money accrueInterest(Instant accrualDate) {
        validateParameter(accrualDate, "Accrual date");
        
        if (accrualDate.isBefore(this.lastInterestAccrualDate)) {
            throw new IllegalArgumentException("Accrual date cannot be before last interest accrual date.");
        }
        
        if (this.status != LiabilityStatus.ACTIVE) {
            return Money.ZERO(this.principalBalance.currency());
        }

        Money newlyAccruedAmount = this.calculateAccruedInterest(accrualDate);
        if (newlyAccruedAmount.amount().compareTo(BigDecimal.ZERO) > 0) {
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

    public void reversePayment(PaymentAllocationResult originalPayment, Instant lastValidPaymentDate) {
        validateParameter(originalPayment, "Original payment");
        
        if (!this.principalBalance.currency().equals(originalPayment.principalPaid().currency())) {
            throw new IllegalArgumentException("Payment allocation currency must match the liability's currency preference.");
        }
        
        if (originalPayment.principalPaid().isNegative() || originalPayment.interestPaid().isNegative()) {
            throw new IllegalArgumentException("Payment amount cannot be negative.");
        }

        // Reactivate liability if it was paid off and we're reversing a payment
        System.out.println(originalPayment.interestPaid());
        System.out.println(originalPayment.principalPaid());
        if (this.status == LiabilityStatus.PAID_OFF && 
            (originalPayment.principalPaid().isPositive() || originalPayment.interestPaid().isPositive())) {
            this.status = LiabilityStatus.ACTIVE;
        }
        
        this.principalBalance = this.principalBalance.add(originalPayment.principalPaid());
        this.accruedUnpaidInterest = this.accruedUnpaidInterest.add(originalPayment.interestPaid());
        this.lastPaymentDate = lastValidPaymentDate;

        validateBusinessInvariants();
        
    }
    
    // should change this to be specific detail's variables
    public void updateDetails(LiabilityDetails details) {
        validateParameter(details, "Liability details");
        this.details = details;
    }
    
    public Money getCurrentBalance() {
        return principalBalance.add(accruedUnpaidInterest);
    }
     public boolean isFullyPaid() {
        return getCurrentBalance().amount().compareTo(BigDecimal.ZERO) == 0;
    }

    public void markAsInDefault(Instant defaultDate) {
        validateParameter(defaultDate, "Default date");
        
        if (this.status != LiabilityStatus.ACTIVE) {
            throw new IllegalStateException("Can only mark active liabilities as in default");
        }
        
        this.status = LiabilityStatus.IN_DEFAULT;
        // Maybe track the default date?
    }

    public void markAsClosed(Instant closureDate) {
        validateParameter(closureDate, "Closure date");
        
        if (this.status != LiabilityStatus.PAID_OFF) {
            throw new IllegalStateException("Can only close fully paid liabilities");
        }
        
        this.status = LiabilityStatus.CLOSED;
    }

    public void reactivateFromDefault() {
        if (this.status != LiabilityStatus.IN_DEFAULT) {
            throw new IllegalStateException("Can only reactivate defaulted liabilities");
        }
        
        this.status = LiabilityStatus.ACTIVE;
    }
    
    public boolean isActive() {
        return this.status == LiabilityStatus.ACTIVE;
    }
    
    public Currency getNativeCurrency() {
        return this.originalAmount.currency();
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
    public Instant getLastPaymentDate() {
        return lastPaymentDate;
    }

    public Instant getIncurrenceDate() {
        return incurrenceDate;
    }
    
    public Instant getLastInterestAccrualDate() {
        return lastInterestAccrualDate;
    }
    
    public LiabilityStatus getStatus() {
        return status;
    }

}
