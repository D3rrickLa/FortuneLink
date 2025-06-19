package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
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
    private Money currenctBalance; // how much is left to pay for the liability
    private Percentage interestRate;
    private LocalDate maturityDate;

    public Liability(UUID liabilityId, UUID portfolioId, String name, String description, Money currenctBalance,
            Percentage interestRate, LocalDate maturityDate) {

        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
        Objects.requireNonNull(name, "Liability name cannot be null.");
        Objects.requireNonNull(currenctBalance, "Current Balance cannot be null.");
        Objects.requireNonNull(interestRate, "Interest rate cannot be null.");
        Objects.requireNonNull(maturityDate, "Maturity date cannot be null.");

        if (interestRate.value().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Interest rate must be positive.");
        }

        this.liabilityId = liabilityId;
        this.portfolioId = portfolioId;
        this.name = name;
        this.description = description;
        this.currenctBalance = currenctBalance;
        this.interestRate = interestRate;
        this.maturityDate = maturityDate;
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

    public Money getCurrenctBalance() {
        return currenctBalance;
    }

    public Percentage getInterestRate() {
        return interestRate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }
}
