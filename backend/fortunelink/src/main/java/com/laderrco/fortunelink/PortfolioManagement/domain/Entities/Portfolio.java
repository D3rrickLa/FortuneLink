package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifer;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Assetidentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.PortfolioCurrency;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionType;

// Aggregate Root
public class Portfolio {
    private UUID portfolioId;
    private UUID userId;
    private String name;
    private String description;
    private boolean isPrimary;
    private PortfolioCurrency CurrencyPreference;

    private List<AssetHolding> assets;
    private List<Liability> liabilities;
    private List<Transaction> transactions;
    
    private Instant createdAt;
    private Instant updatedAt;



    public Portfolio(UUID userUuid, String portfolioName, String portfolioDescription, PortfolioCurrency currencyPref, boolean isPrimary) {
        if (userUuid == null) {
            throw new IllegalArgumentException("Portfolio must have a User assigned to it.");
        }

        if (portfolioName == null || portfolioName.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio must be given a name.");
        }

        if (currencyPref == null) {
            throw new IllegalArgumentException("Portfolio must have a currency preference.");
        }
        
        this.userId = userUuid;
        this.name = portfolioName;
        this.description = portfolioDescription;
        this.CurrencyPreference = currencyPref;

        this.isPrimary = isPrimary;

        this.portfolioId = UUID.randomUUID();


        this.assets = new ArrayList<>();
        this.liabilities = new ArrayList<>();
        this.transactions = new ArrayList<>();


        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updatePortfolioDescription(String newDescription) {
        this.description = newDescription; // Allow null/empty for description
        this.updatedAt = Instant.now();
    }

    public void renamePortfolio(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio name cannot be null or empty.");
        }

        this.name = newName;
        this.updatedAt = Instant.now();
    }

    public void setPrimaryStatus(boolean newStatus) {
        // no need to check, since we are using prim, will default to false
        this.isPrimary = newStatus;
    }

    // Asset Holding Management -> everything we can do with an asset
    public AssetHolding recordAssetPurchase(UUID portfolioUuid, AssetIdentifier assetIdentifer, BigDecimal quantity, Money costBasisPerUnit, LocalDate acquisitionDate, Money currenyMarketPrice) {
        return null;
    }

    public void recordAssetSale(AssetIdentifier assetIdentifer, BigDecimal quantityToSell, Money salePricePerUnit) {

    }

    public void removeAssetHolding(UUID assetHolding) {

    }

    // Liability Management (i.e. Debt)
    public Liability addLiability(UUID liabilityId, String name, Money initialAmount, Percentage interestRate, LocalDate maturityDate) {
        return null;
    }

    public void recordLiabilityPayment(UUID liabilityId, Money paymentAmount) {

    }

    public void removeLiability(UUID liabilityId) {

    }

    // Transaction Management (for non-asset/liability related, or voiding existing ones)
    public void recordCashTransaction(UUID transactionId, TransactionType type, Money amount, String description) {

    }

    public void voidTransaction(UUID transactionId, String reason) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Portfolio portfolio = (Portfolio) o;
        return portfolioId != null && portfolioId.equals(portfolio.portfolioId); // Equality based on ID
    }

    @Override
    public int hashCode() {
        return portfolioId != null ? portfolioId.hashCode() : 0;
    }


    public UUID getPortfolioId() {
        return portfolioId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public PortfolioCurrency getCurrencyPreference() {
        return CurrencyPreference;
    }

    public List<AssetHolding> getAssets() {
        return assets;
    }

    public List<Liability> getLiabilities() {
        return liabilities;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
}
