package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

// might need a way to change the currency of the liability 
public class Liability {
    private final UUID liabilityId;
    private final UUID portfolioId;
    private String name;
    private String description;
    private Money currentBalance;
    private Percentage annualInterestRate;
    private Instant maturityDate;
    private Instant lastInterestAccrualDate ; // when was interest last calculated.
    private Currency liabilityCurrencyPreference;
    
    public Liability(
        UUID liabilityId, 
        UUID portfolioId, 
        String name,
        String description, 
        Money currentBalance,
        Percentage annualInterestRate,
        Instant maturityDate,
        Instant lastInterestAccrualDate,
        Currency liabilityCurrencyPreference
    ) {
        this.liabilityId = liabilityId;
        this.portfolioId = portfolioId;
        this.name = name;
        this.description = description;
        this.currentBalance = currentBalance;
        this.annualInterestRate = annualInterestRate;
        this.maturityDate = maturityDate;
        this.lastInterestAccrualDate = lastInterestAccrualDate;
        this.liabilityCurrencyPreference = liabilityCurrencyPreference;
    }

    public void applyPayment(Money paymentAmount) {
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null.");
        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be a positive number.");
        }
        if (!liabilityCurrencyPreference.equals(paymentAmount.currency())) {
            throw new IllegalArgumentException("Payment amount must be in the same currency as liability's currency preference.");
        }
        this.currentBalance = this.currentBalance.subtract(paymentAmount);
    }
 
    public void reversePaymentEffect(Money reverseAmount) {
        Objects.requireNonNull(reverseAmount, "Reverse amount cannot be null.");
        if (reverseAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reverse amount must be a positive number.");
        }
        if (!liabilityCurrencyPreference.equals(reverseAmount.currency())) {
            throw new IllegalArgumentException("Reverse amount must be in the same currency as liability's currency preference.");
        }
        this.currentBalance = this.currentBalance.add(reverseAmount);
    }
    
    public void increaseLiabilityBalance(Money amount) {
        Objects.requireNonNull(amount, "Amount to increase liability balance cannot be null.");
        if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to increase liability balance must be a positive number.");
        }
        if (!liabilityCurrencyPreference.equals(amount.currency())) {
            throw new IllegalArgumentException("Amount to increase liability balance must be the same currency as the liability currency preference.");
        }
        this.currentBalance = this.currentBalance.add(amount);
    }

    public Money calculateAccuredInterest() {
        // Calculate interest since lastInterestAccualDate
        // Return the calculcated amount without modifying state

        // get instant.now(), subtract from lastInterestAccualDate, that number is how many days of interest we have
        // we are assuming interest is calculated daily

        Instant currentDate = Instant.now();
        
        long daysBetween = ChronoUnit.DAYS.between(this.lastInterestAccrualDate, currentDate);
        if (daysBetween <= 0) {
            return Money.ZERO(liabilityCurrencyPreference);
        }
        // annual rate / 365 = daily rate
        BigDecimal dailyRate = this.annualInterestRate.toDecimal().divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_EVEN);

        // Interest = Principal x Daily Rate x Number of Days
        BigDecimal interestAmount = this.currentBalance.amount().multiply(dailyRate).multiply(BigDecimal.valueOf(daysBetween));

        return new Money(interestAmount, liabilityCurrencyPreference);
    }

    public Money accureInterest() {
        Money accuredAmount = calculateAccuredInterest();
        this.currentBalance = this.currentBalance.add(accuredAmount);
        this.lastInterestAccrualDate = Instant.now();
        return accuredAmount;
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

    public Money getCurrentBalance() {
        return currentBalance;
    }

    public Percentage getAnnualInterestRate() {
        return annualInterestRate;
    }

    public Instant getMaturityDate() {
        return maturityDate;
    }

    public Instant getLastInterestAccrualDate() {
        return lastInterestAccrualDate;
    }

    public Currency getLiabilityCurrencyPreference() {
        return liabilityCurrencyPreference;
    }
    
}
