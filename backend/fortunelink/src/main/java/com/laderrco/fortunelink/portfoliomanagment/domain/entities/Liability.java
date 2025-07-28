package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.PaymentAllocationResult;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;

// might need a way to change the currency of the liability 
public class Liability {
    private final UUID liabilityId;
    private final UUID portfolioId;
    private String name;
    private String description;
    private Money originalAmount;
    private Money currentBalance;
    private Percentage annualInterestRate;
    private Instant incurrenceDate;
    private Instant maturityDate;
    private Instant lastInterestAccrualDate ; // when was interest last calculated.
    private Money accuredInterestUnpaidInLiabilityCurrency;
    
    public Liability(
        UUID liabilityId, 
        UUID portfolioId, 
        String name,
        String description, 
        Money originalAmount,
        Money currentBalance, // in liability currency
        Percentage annualInterestRate,
        Instant incurrenceDate,
        Instant maturityDate,
        Instant lastInterestAccrualDate
    ) {
        Objects.requireNonNull(liabilityId, "Liability id cannot be null.");
        Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        Objects.requireNonNull(name, "Name cannot be null.");
        Objects.requireNonNull(originalAmount, "Initial Liability balance cannot be null.");
        Objects.requireNonNull(currentBalance, "Liability balance cannot be null.");
        Objects.requireNonNull(annualInterestRate, "Annual interest rate cannot be null.");
        Objects.requireNonNull(incurrenceDate, "Incurrence date cannot be null.");
        Objects.requireNonNull(maturityDate, "Maturity date cannot be null.");
        Objects.requireNonNull(lastInterestAccrualDate, "Last interest accrual date cannot be null.");

        if (currentBalance.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial liability balance cannot be a negative value.");
        }

        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Liability name cannot be blank.");
        }

        this.liabilityId = liabilityId;
        this.portfolioId = portfolioId;
        this.name = name;
        this.description = description.trim();
        this.originalAmount = originalAmount;
        this.currentBalance = currentBalance;
        this.annualInterestRate = annualInterestRate;
        this.incurrenceDate = incurrenceDate;
        this.maturityDate = maturityDate;
        this.lastInterestAccrualDate = lastInterestAccrualDate;
        this.accuredInterestUnpaidInLiabilityCurrency = Money.ZERO(currentBalance.currency()); // Initialize

    }

    public PaymentAllocationResult applyPayment(Money paymentAmount, Instant paymentDate) {
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null.");
        Objects.requireNonNull(paymentDate, "Payment date cannot be null.");

        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be a positive number.");
        }
        if (!this.currentBalance.currency().equals(paymentAmount.currency())) {
            throw new IllegalArgumentException("Payment amount must be in the same currency as liability's currency preference.");
        }

        // --- IMPORTANT: Ensure interest is accrued up to the payment date before applying payment ---
        // This is crucial to ensure that any interest that has accrued since the last official accrual
        // is added to the accruedInterestUnpaid before the payment is processed.
        // If your Portfolio.accrueInterestLiabilities runs frequently enough (e.g., daily),
        // and you're confident this.accruedInterestUnpaid is always up-to-date, you *might* skip this.
        // However, for robust financial systems, it's safer to ensure state consistency here.
        this.accrueInterest(paymentDate); // Assuming this method updates accruedInterestUnpaid and currentBalance

        Money interestPaid = Money.ZERO(paymentAmount.currency());
        Money principalPaid = Money.ZERO(paymentAmount.currency());
        Money remainingPayment = paymentAmount;


        // 1. Apply payment to unpaid interest first
        // If there's outstanding accrued interest
        if (this.accuredInterestUnpaidInLiabilityCurrency.amount().compareTo(BigDecimal.ZERO) > 0) {
            if (remainingPayment.amount().compareTo(this.accuredInterestUnpaidInLiabilityCurrency.amount()) >= 0) {
                // Payment covers all unpaid interest
                interestPaid = this.accuredInterestUnpaidInLiabilityCurrency;
                remainingPayment = remainingPayment.subtract(this.accuredInterestUnpaidInLiabilityCurrency);
                this.accuredInterestUnpaidInLiabilityCurrency = Money.ZERO(this.accuredInterestUnpaidInLiabilityCurrency.currency()); // Interest fully paid
            } else {
                // Payment covers only a portion of unpaid interest
                interestPaid = remainingPayment;
                this.accuredInterestUnpaidInLiabilityCurrency = this.accuredInterestUnpaidInLiabilityCurrency.subtract(remainingPayment);
                remainingPayment = Money.ZERO(paymentAmount.currency()); // Payment consumed by interest
            }
        }

        // 2. Apply any remaining payment to principal
        if (remainingPayment.amount().compareTo(BigDecimal.ZERO) > 0) {
            principalPaid = remainingPayment; // The rest goes to principal
        }
        
        // 3. Deduct the *total actual payment* from the currentBalance.
        // This currentBalance already includes principal + accrued interest.
        // The individual reductions (interestPaid, principalPaid) are for tracking/reporting purposes.
        this.currentBalance = this.currentBalance.subtract(paymentAmount);

        // 4. Handle overpayment scenario (optional, but good for robustness)
        if (this.currentBalance.amount().compareTo(BigDecimal.ZERO) < 0) {
            // This means the payment was more than the total outstanding balance.
            // The excess effectively reduces the "principal paid" to avoid a negative balance.
            principalPaid = principalPaid.add(this.currentBalance.negate()); // Add the 'negative balance' back to principal paid
            this.currentBalance = Money.ZERO(currentBalance.currency()); // Set balance to zero
        }

        return new PaymentAllocationResult(principalPaid, interestPaid, this.currentBalance);
    }
 
    public void reversePaymentEffect(Money reverseAmount) {
        Objects.requireNonNull(reverseAmount, "Reverse amount cannot be null.");
        if (reverseAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reverse amount must be a positive number.");
        }
        if (!this.currentBalance.currency().equals(reverseAmount.currency())) {
            throw new IllegalArgumentException("Reverse amount must be in the same currency as liability's currency preference.");
        }
        this.currentBalance = this.currentBalance.add(reverseAmount);
    }
    
    public void increaseLiabilityBalance(Money amount) {
        Objects.requireNonNull(amount, "Amount to increase liability balance cannot be null.");
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to increase liability balance must be a positive number.");
        }
        if (!this.currentBalance.currency().equals(amount.currency())) {
            throw new IllegalArgumentException("Amount to increase liability balance must be the same currency as the liability currency preference.");
        }
        this.currentBalance = this.currentBalance.add(amount);
    }

    public void reduceLiabilityBalance(Money amount) {
        Objects.requireNonNull(amount, "Amount to reduce cannot be null.");
        if (!this.currentBalance.currency().equals(amount.currency())) {
            throw new IllegalArgumentException("Reduction amount currency must match liability currency.");
        }
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) { // Only allow positive reductions
            throw new IllegalArgumentException("Reduction amount must be positive.");
        }
        this.currentBalance = this.currentBalance.subtract(amount);
    }

    public Money calculateAccruedInterest(Instant asOfDate) {
        // interst if based on principal portion
        Objects.requireNonNull(asOfDate, "As-of date cannot be null.");
        
        // If the asOfDate is before or equal to the last accrual date, no new interest has accrued.
        if (asOfDate.isBefore(this.lastInterestAccrualDate) || asOfDate.equals(this.lastInterestAccrualDate)) {
            return Money.ZERO(this.currentBalance.currency());
        }

        long daysBetween = ChronoUnit.DAYS.between(this.lastInterestAccrualDate, asOfDate);
        if (daysBetween <= 0) { // Safety check, though covered by above
            return Money.ZERO(this.currentBalance.currency());
        }
        
        Money principalForCalculation = this.currentBalance.subtract(this.accuredInterestUnpaidInLiabilityCurrency);
        if (principalForCalculation.amount().compareTo(BigDecimal.ZERO) < 0) {
            principalForCalculation = Money.ZERO(principalForCalculation.currency()); // Principal cannot be negative for interest calculation
        }

        BigDecimal dailyRate = this.annualInterestRate.percentageValue()
                                   .divide(BigDecimal.valueOf(365), DecimalPrecision.PERCENTAGE.getDecimalPlaces(), RoundingMode.HALF_UP);

        // Interest = Principal x Daily Rate x Number of Days
        BigDecimal interestAmountValue = principalForCalculation.amount()
                                             .multiply(dailyRate)
                                             .multiply(BigDecimal.valueOf(daysBetween));

        return new Money(interestAmountValue, this.currentBalance.currency());
    }

    public Money accrueInterest(Instant accrualDate) {
        Objects.requireNonNull(accrualDate, "Accrual date cannot be null.");

        // Calculate the amount to accrue using the flexible calculateAccruedInterest method
        Money newlyAccruedAmount = this.calculateAccruedInterest(accrualDate);

        if (newlyAccruedAmount.amount().compareTo(BigDecimal.ZERO) > 0) {
            // 1. Update the total outstanding balance
            this.currentBalance = this.currentBalance.add(newlyAccruedAmount);

            // 2. CRUCIALLY: Update the tracking of unpaid accrued interest
            this.accuredInterestUnpaidInLiabilityCurrency = this.accuredInterestUnpaidInLiabilityCurrency.add(newlyAccruedAmount);

            // 3. Update the last accrual date to reflect that interest has been processed up to this point
            this.lastInterestAccrualDate = accrualDate;
        }

        return newlyAccruedAmount; // Return the amount that was actually accrued

    }
    
    public void setName(String name) {
        Objects.requireNonNull(name, "New liability name cannot be null.");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("New liability name cannot be blank.");
        }
        this.name = name;
    }
    public void setDescription(String description) {
        description = description.trim();
        this.description = description;
    }
    public void setAnnualInterestRate(Percentage annualInterestRate) {
        Objects.requireNonNull(annualInterestRate, "New annual interest rate cannot be null.");
        this.annualInterestRate = annualInterestRate;
    }

    public void setLastAccrualDate(Instant newDate) {
        this.lastInterestAccrualDate = newDate;
    }

    public UUID getLiabilityId() {
        return liabilityId;
    }

    public UUID getPortfolioId() {
        return portfolioId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
    public Money getOriginalAmount() { 
        return originalAmount; 
    } 

    public Money getCurrentBalance() {
        return currentBalance;
    }

    public Percentage getAnnualInterestRate() {
        return annualInterestRate;
    }

    public Instant getIncurrenceDate() { 
        return incurrenceDate; 
    }

    public Instant getMaturityDate() {
        return maturityDate;
    }

    public Instant getLastInterestAccrualDate() {
        return lastInterestAccrualDate;
    }

    public Currency getLiabilityCurrencyPreference() {
        return this.currentBalance.currency();
    }

    public Money getAccuredInterestUnpaidInLiabilityCurrency() {
        return accuredInterestUnpaidInLiabilityCurrency;
    }    
}
