package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.util.Collections;
import java.util.Currency;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;

public class User {
    private UserId id;
    private String displayName;
    private Currency reportingCurrencyPreference;
    private Set<PortfolioId> portfolioIds;
    
    private User(UserId id, String displayName, Currency reportingCurrencyPreference, Set<PortfolioId> portfolioIds) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.reportingCurrencyPreference = Objects.requireNonNull(reportingCurrencyPreference);
        this.portfolioIds = Objects.requireNonNull(new HashSet<>(portfolioIds)); // Defensive copy
    }

    public static User createNew(UserId id, String displayName, Currency reportingCurrencyPreference) {
        // 'id' comes from the external user ID (Supabase)
        return new User(id, displayName, reportingCurrencyPreference, new HashSet<>());
    }

    // --- Behavioral methods ---
    public void addPortfolio(PortfolioId portfolioId) {
        Objects.requireNonNull(portfolioId, "PortfolioId cannot be null");
        this.portfolioIds.add(portfolioId);
    }

    public void removePortfolio(PortfolioId portfolioId) {
        Objects.requireNonNull(portfolioId, "PortfolioId cannot be null");
        this.portfolioIds.remove(portfolioId);
    }

    public void updateReportingCurrencyPreference(Currency newPreference) {
        Objects.requireNonNull(newPreference, "Reporting currency preference cannot be null");
        this.reportingCurrencyPreference = newPreference;
    }

    public void updateDisplayName(String newDisplayName) {
        Objects.requireNonNull(newDisplayName, "Display name cannot be null");
        if (newDisplayName.isBlank()) throw new IllegalArgumentException("Display name cannot be blank");
        this.displayName = newDisplayName;
    }

    

    public UserId getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Currency getReportingCurrencyPreference() {
        return reportingCurrencyPreference;
    }

    public Set<PortfolioId> getPortfolioIds() {
        return Collections.unmodifiableSet(portfolioIds);
    }

    // equals and hashCode would be based on 'id'
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

