package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

// Aggregate Root
public class Portfolio {
    private UUID portfolioId;
    private UUID userId;
    private String name;
    private String description;
    private boolean isPrimary;
    private Currency CurrencyPreference;

    private List<AssetHolding> assets;
    private List<Liability> liabilities;
    private List<Transaction> transactions;
    
    private Instant creationDate;
    private Instant updatedAt;

    // what can a Portfolio do?
    /*
     * Update itself with the holdings
     * Remove holdings
     * be renamed to something
     * it has boundaries, so anything connected to it should be put here
     * 
     * 
     * NOTE: what can the ROOT do? not what can the user do with the ROOT
     */

    public Portfolio(UUID portfolioUuid, UUID userUuid, String name) {
        this.portfolioId = portfolioUuid;
        this.userId = userUuid;
        this.name = name;
    }

    public void setPrimaryPortfolio(boolean isPrimary) {
        this.isPrimary = isPrimary;
        this.updatedAt = Instant.now();
        // If you only allow one primary portfolio per user, this logic would live
        // in an Application Service or Domain Service that queries other portfolios
        // owned by the user and sets their 'isPrimary' to false.
        // The Portfolio itself can only set *its own* status.
    }
    
    
    public void updateDescription(String newDescription) {
        this.description = newDescription; // Allow null/empty for description
        this.updatedAt = Instant.now();
    }
    
    public void renamePortfolio(String newName) {
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    public AssetHolding recordNewAssetPurchase() {
        return null;
    }

    public void recordAssetSale() {
        
    }

    public Liability addLiability() {
        return null;
    }

    public void recordNewLiabilityPayment() {

    }

    // for non asset/liability transaction
    public void recordCashTransaction() {

    }

    // need the deletion methods here
    public void removeAssetHolding(UUID assetUuid) {
        // remove logic
    }

    public void removeLiability(UUID liabilityUuid) {
        // remove logic
    }

    public void voidTransaction(UUID tranctionId, String reason) {

    }


    // public void CalculateNetWorth(Function<AssetIdentifer, Money> cure)

    // --- equals() and hashCode() ---
    // Critical for Entities to ensure proper comparison and collection behavior.
    // Typically based on the primary key (portfolioId).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Portfolio portfolio = (Portfolio) o;
        return portfolioId != null && portfolioId.equals(portfolio.portfolioId); // Equality based on ID
    }

    @Override
    public int hashCode() {
        return portfolioId != null ? portfolioId.hashCode() : 0;
    }
}
