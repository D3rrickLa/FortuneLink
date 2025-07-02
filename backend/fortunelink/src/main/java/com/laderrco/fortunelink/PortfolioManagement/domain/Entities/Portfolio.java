package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class Portfolio {
    private final UUID portfolioId;
    private final UUID userId; // FK from supabase
    private String portfolioName;
    private String portfolioDescription;
    private PortfolioCurrency portfolioCurrencyPreference; // your currency preference can change
    private Money portfolioCashBalance; // cash in your portfolio, we need to make sure you have cash to spend on
                                        // assets, else you can't make new transaction
    private final ExchangeRateService exchangeRateService;

    private final Instant createdAt;
    private Instant updatedAt;

    private List<Transaction> transactions;
    private List<AssetHolding> assetHoldings;
    private List<Liability> liabilities;

    public Portfolio(final UUID portfolioId, final UUID userId, String portfolioName, String portfolioDescription,
            PortfolioCurrency portfolioCurrencyPreference, Money portfolioCashBalance,
            ExchangeRateService exchangeRateService, Instant createdAt) {

        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(userId, "User ID cannot be null.");
        Objects.requireNonNull(portfolioName, "Portfolio Name cannot be null.");
        Objects.requireNonNull(portfolioCurrencyPreference, "Portfolio Currency Preference cannot be null.");
        Objects.requireNonNull(portfolioCashBalance, "Cash Balance cannot be null.");
        Objects.requireNonNull(createdAt, "Creation date cannot be null.");

        if (portfolioName.trim().isEmpty()) {
            throw new IllegalArgumentException("The portfolio name cannot be empty.");
        }

        if (portfolioCashBalance.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Cannot have a cash balance less than 0.");
        }

        if (!portfolioCashBalance.currency().javaCurrency().equals(portfolioCurrencyPreference.javaCurrency())) {
            throw new IllegalArgumentException("Cash depositied does not match your currency preference.");
        }

        this.portfolioId = portfolioId;
        this.userId = userId;
        this.portfolioName = portfolioName;
        this.portfolioDescription = (portfolioDescription != null) ? portfolioDescription : "";
        this.portfolioCurrencyPreference = portfolioCurrencyPreference;
        this.portfolioCashBalance = portfolioCashBalance;
        this.exchangeRateService = exchangeRateService;
        this.createdAt = createdAt;
        this.updatedAt = Instant.now();

        this.transactions = new ArrayList<>();
        this.assetHoldings = new ArrayList<>();
        this.liabilities = new ArrayList<>();
    }

    public void recordCashflow(TransactionType type, Money cashflowAmount, Instant cashflowEventDate, TransactionMetadata transactionMetadata, List<Fee> fees) {

    }


    public AssetHolding recordAssetHoldingPurchase(AssetIdentifier assetIdentifier, BigDecimal quantityOfAssetBought, Instant acquisitionDate, Money rawPricePerUnit, TransactionMetadata transactionMetadata, List<Fee> fees) {
        return null;
    }

    public void recordAssetHoldingSale(AssetIdentifier assetIdentifier, BigDecimal quantityToSell, Instant saleDate,
            Money rawSalePricePerUnit, TransactionMetadata transactionMetadata, List<Fee> fees) {

    }

    public void recordLiabilityPayment(final UUID liabilityId, Money paymentAmount,
            Instant transactionDate, TransactionMetadata transactionMetadata, List<Fee> fees) {
    }

   public void voidTransaction(final UUID transactionIdToVoid, String reason) {

    }
    
    // --- Getter Methods --- //
    public UUID getPortfolioId() {
        return portfolioId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPortfolioName() {
        return portfolioName;
    }

    public String getPortfolioDescription() {
        return portfolioDescription;
    }

    public PortfolioCurrency getPortfolioCurrencyPreference() {
        return portfolioCurrencyPreference;
    }

    public Money getPortfolioCashBalance() {
        return portfolioCashBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // returning unmodifiable views, preventing external code from modifying the
    // portfolio's internal state directly
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public List<AssetHolding> getAssetHoldings() {
        return Collections.unmodifiableList(assetHoldings);
    }

    public List<Liability> getLiabilities() {
        return Collections.unmodifiableList(liabilities);
    }

}
