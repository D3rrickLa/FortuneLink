package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Percentage;

public class Liability {
    private final UUID liabilityId;
    private final UUID portfolioId;
    private String liabilityName;
    private String liabilityDescription;
    private Money currentLiabilityBalance;
    private Percentage interestRate;
    private ZonedDateTime maturityDate;

    public Liability(final UUID liabilityId, final UUID portfolioId, String liabilityName, String liabilityDescription,
            Money currentLiabilityBalance, Percentage interestRate, ZonedDateTime maturityDate) {

        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
        Objects.requireNonNull(liabilityName, "Liability name cannot be null.");
        Objects.requireNonNull(currentLiabilityBalance, "Current Balance cannot be null.");
        Objects.requireNonNull(interestRate, "Interest rate cannot be null.");
        Objects.requireNonNull(maturityDate, "Maturity date cannot be null.");

        if (currentLiabilityBalance.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial balance for liability must be positive.");
        }

        this.liabilityId = liabilityId;
        this.portfolioId = portfolioId;
        this.liabilityName = liabilityName;
        this.liabilityDescription = liabilityDescription;
        this.currentLiabilityBalance = currentLiabilityBalance;
        this.interestRate = interestRate;
        this.maturityDate = maturityDate;
    }

    /*
     * What can Liability do?
     * VERBS
     * - change name
     * - change description
     * - update interest rate
     * - update maturity date
     * - make payment
     * - back pay liability
     * 
     * NOUNS
     * e.g. represents a debt you have whether that is car loans, mortage, student
     * loans, etc.
     */

    public void changeLiabilityName(String name) {
        Objects.requireNonNull(name, "Liability name cannot be changed to null.");
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Liability name cannot be empty.");
        }

        this.liabilityName = name;
    }

    public void changeLiabilityDescription(String description) {
        this.liabilityDescription = description;
    }

    public void updateInterestRate(Percentage newInterestRate) {
        Objects.requireNonNull(newInterestRate, "Interest rate cannot be null");

        // NOTE: we don't need this because Percentage class already has a check in
        // place for this
        // if (newInterestRate.percentValue().compareTo(BigDecimal.ZERO) < 0) {
        // throw new IllegalArgumentException("Interest rate cannot be less than
        // zero.");
        // }
        this.interestRate = newInterestRate;
    }

    public void updateMaturityDate(ZonedDateTime updatedDateTime) {
        Objects.requireNonNull(updatedDateTime, "Maturity date cannot be null");
        this.maturityDate = updatedDateTime;
    }

    public void makeLiabilityPayment(Money newPayment) {
        Objects.requireNonNull(newPayment, "Amount to pay off Liability balance cannot be null.");

        if (newPayment.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount to pay off Liability balance cannot be less than or equal to zero.");
        }

        else if (!this.currentLiabilityBalance.currency().code().equals(newPayment.currency().code())) {
            throw new IllegalArgumentException("Payment amount must be the same currency with the original liability.");
        }

        else if (newPayment.amount().compareTo(this.currentLiabilityBalance.amount()) > 0) {
            throw new IllegalArgumentException(String.format("%s excees the current liability balance of %s for %s",
                    newPayment, this.currentLiabilityBalance, this.liabilityName));
        }
        this.currentLiabilityBalance = new Money(this.currentLiabilityBalance.amount().subtract(newPayment.amount()),
                this.currentLiabilityBalance.currency());

    }

    public void increaseLiabilityBalance(Money amountToIncrease) {
        Objects.requireNonNull(amountToIncrease, "Amount to increase Liability balance cannot be null.");

        if (amountToIncrease.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount to increase Liability balance cannot be less than or equal to zero.");
        }

        else if (!this.currentLiabilityBalance.currency().code().equals(amountToIncrease.currency().code())) {
            throw new IllegalArgumentException("Increase value must be the same currency with the original liability.");
        }

        this.currentLiabilityBalance = new Money(this.currentLiabilityBalance.amount().add(amountToIncrease.amount()),
                this.currentLiabilityBalance.currency());

    }

    // --- GETTERS --- //
    public UUID getLiabilityId() {
        return liabilityId;
    }

    public UUID getPortfolioId() {
        return portfolioId;
    }

    public String getLiabilityName() {
        return liabilityName;
    }

    public String getLiabilitDescription() {
        return liabilityDescription;
    }

    public Money getCurrentLiabilityBalance() {
        return currentLiabilityBalance;
    }

    public Percentage getInterestRate() {
        return interestRate;
    }

    public ZonedDateTime getMaturityDate() {
        return maturityDate;
    }

    public void reversePayment(Money amountAppliedToLiability) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reversePayment'");
    }

}
