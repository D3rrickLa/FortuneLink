package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;

public class Liability {
    private UUID liabilityId;
    private UUID portfolioId;
    private String name;
    private String description;
    private Money currentBalance; // how much is left to pay for the liability
    private Percentage interestRate;
    private LocalDate maturityDate;

    private Instant createdAt;
    private Instant updatedAt;

    public Liability(UUID liabilityId, UUID portfolioId, String name, String description, Money currentBalance,
            Percentage interestRate, LocalDate maturityDate) {

        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
        Objects.requireNonNull(name, "Liability name cannot be null.");
        Objects.requireNonNull(currentBalance, "Current Balance cannot be null.");
        Objects.requireNonNull(interestRate, "Interest rate cannot be null.");
        Objects.requireNonNull(maturityDate, "Maturity date cannot be null.");

        // Add check for currentBalance being positive on creation (it's the initial
        // amount)
        if (currentBalance.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial balance for liability must be positive.");
        }

        this.liabilityId = liabilityId;
        this.portfolioId = portfolioId;
        this.name = name;
        this.description = description;
        this.currentBalance = currentBalance;
        this.interestRate = interestRate;
        this.maturityDate = maturityDate;
        this.createdAt = Instant.now(); // NOTE: we would want to fix this, because we could be creating liabilities from the past
        this.updatedAt = Instant.now();
    }

    // AI assisted
    public void makePayment(Money paymentAmount) {
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null.");

        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) { // can't make a payment of 0 dollars or less
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        // Ensure payment currency matches liability's balance currency
        if (!this.currentBalance.currencyCode().equals(paymentAmount.currencyCode())) {
            throw new IllegalArgumentException("Payment currency mismatch with liability's balance currency.");
        }

        // Prevent overpayment beyond current balance (unless overpayment is a business
        // rule)
        if (paymentAmount.amount().compareTo(this.currentBalance.amount()) > 0) {
            // Option 1: Throw an error (common for MVP)
            throw new IllegalArgumentException("Payment amount " + paymentAmount + " exceeds current liability balance "
                    + this.currentBalance + " for " + this.name + ".");
            // Option 2: Adjust payment to just cover the remaining balance, and perhaps
            // return change or record credit
            // paymentAmount = this.currentBalance;
        }

        this.currentBalance = this.currentBalance.subtract(paymentAmount);
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Liability that = (Liability) o;
        return liabilityId != null && liabilityId.equals(that.liabilityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(liabilityId);
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

    public Percentage getInterestRate() {
        return interestRate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
