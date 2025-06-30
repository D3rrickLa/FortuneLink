package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.factories.TransactionFactory;
import com.laderrco.fortunelink.portfoliomanagement.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Percentage;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;


public class Portfolio {
    private final UUID portfolioId;
    private final UUID userId; // FK from supabase
    private String portfolioName;
    private String portfolioDescription;
    private PortfolioCurrency portfolioCurrencyPreference; // your currency preference can change
    private Money portfolioCashBalance; // cash in your portfolio, we need to make sure you have cash to spend on assets, else you can't make new transaction
    private final ExchangeRateService exchangeRateService;

    private final Instant createdAt;
    private Instant updatedAt;
    
    private List<Transaction> transactions;
    private List<AssetHolding> assetHoldings;
    private List<Liability> liabilities;
    
    public Portfolio(final UUID portfolioId, final UUID userId, String portfolioName, String portfolioDescription, PortfolioCurrency portfolioCurrencyPreference, Money portfolioCashBalance, ExchangeRateService exchangeRateService, Instant createdAt) {
        
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
        /*
         * --CHECKS NEEDED--
         * Null checks
         * Is Cashflow amount negative or 0 
         * Is cashflow the same currency as the portfolio preference
         * Is Cashflow type either deposit, withdrawl, interest, or dividend
         * is status == completed
         * is withdrawal too much
         * check if fees are in the same currency as portfolio 
        */
        
        Objects.requireNonNull(type, "Transaction type cannot be null.");
        Objects.requireNonNull(cashflowAmount, "Amount of cash being put in/ taken out cannot be null.");
        Objects.requireNonNull(cashflowEventDate, "Cash transaction date cannot be null.");
        Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");

        if (!Set.of(TransactionType.DEPOSIT, TransactionType.WITHDRAWAL, TransactionType.INTEREST, TransactionType.DIVIDEND).contains(type)) {
            throw new IllegalArgumentException("Transaction Type must be either DEPOSIT, WITHDRAWAL, INTEREST, or DIVIDEND.");
        }

        if (cashflowAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cash amount for " + type + " cannot less than or equal to zero.");
        }

        if (!transactionMetadata.transactionStatus().equals(TransactionStatus.COMPLETED)) {
            throw new IllegalArgumentException("Status in metadata must be COMPLETED.");
        }

        BigDecimal exchangeRate;
        BigDecimal conversionFeeAmount;
        // TODO, fix up the multi-currency
        if (cashflowAmount.currency().javaCurrency().equals(this.portfolioCurrencyPreference.javaCurrency())) {
            exchangeRate = BigDecimal.ONE;
            conversionFeeAmount = BigDecimal.ZERO;
        }
        else {
            exchangeRate = exchangeRateService.getCurrencyExchangeRate(cashflowAmount.currency().javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(), cashflowEventDate)
        
            // assuming that the fees parameter has a EXCHANGE_RATE fee for conversionFee
        }
    }
    
    public AssetHolding recordAssetHoldingPurchase(AssetIdentifier assetIdentifier, BigDecimal quantityOfAssetBought, Instant acquisitionDate, Money pricePerUnit, TransactionMetadata transactionMetadata, List<Fee> fees) {
       return null;
        
    }
    
    public void recordAssetHoldingSale(AssetIdentifier assetIdentifier, BigDecimal quantityToSell, Money salePricePerUnit) {

    }

    public Liability recordNewLiability(final UUID portfolioId, String liabilityName, String liabilityDescription, Money initialOutstandingBalance, Percentage interestRate, Instant maturityDate) {
        return null;
    }

    public void recordLiabilityPayment(final UUID portfolioId, final UUID liabilityId, Money paymentAmount, Instant transactionDate) {

    }

    public void voidTransaction(final UUID transactionId, String reason) {

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

    // returning unmodifiable views, preventing external code from modifying the portfolio's internal state directly
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
