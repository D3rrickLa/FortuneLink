package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;

public class Liability {
    private final UUID liabilityId;
    private final UUID portfolioId;
    private String liabilityName;
    private String liabilityDescription;
    private Money currentLiabilityBalance;
    private Percentage interestRate;
    private ZonedDateTime maturityDate;

    public Liability(final UUID liabilityId, final UUID portfolioId, String liabilityName, String liabilityDescription, Money currentLiabilityBalance, Percentage interestRate, ZonedDateTime maturityDate) {
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
     * 
     * NOUNS
     * e.g. represents a debt you have whether that is car loans, mortage, student loans, etc. 
     */

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

}
