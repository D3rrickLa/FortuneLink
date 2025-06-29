package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.CashTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Fee;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionMetadata;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionType;
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
    
    private final Instant createdAt;
    private Instant updatedAt;
    
    private List<Transaction> transactions;
    private List<AssetHolding> assetHoldings;
    private List<Liability> liabilities;
    
    public Portfolio(final UUID portfolioId, final UUID userId, String portfolioName, String portfolioDescription, PortfolioCurrency portfolioCurrencyPreference, Money portfolioCashBalance, Instant createdAt) {
        
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
        this.createdAt = createdAt;
        this.updatedAt = Instant.now();

        this.transactions = new ArrayList<>();
        this.assetHoldings = new ArrayList<>();
        this.liabilities = new ArrayList<>();
    }

    /*
     * Behaviour - what can a portfolio do?
     * in our case we have 3 items that interact with porfolio (Transaction, AssetHolding, Liability)
     * so we need to have methods for adding and 'removing' said things from each entity
     * we also need to deal with cashflow, how can we get cash in to/out of our portfolio?
     * - deposit
     * - withdrawl
     * - dividend
     * - interest
     * 
     * for such behaviours, whenever you are stuck, think of 'verbs' related to the Portoflio and things you want to do
     * i.e. buying/sell stocks
     * i.e. depositing/withdrawing money
     * etc.
     * 
     * for what is a portfolio? think of 'nouns'
     * i.e. it's a place to containizer all your finances so Cash in portfolio, investments, liabilities, transactions, etc.
     * 
     * NOTE: 
     * since we are pulling our 'funds' from 1 cash balance, we need to deal with currency conversion, so whenever someone enters a trade that isn't today,
     * we need to pull historical info for that exchange rate. Think of this like WealthSimple without a USD account, we can buy only with CAD, but we can convert
     * our CAD to USD to buy something else. This is in contrast where we have dedicated accounts for USD and CAD
    */


    // what do we need to to add/remove cash to our account? actual cash
    public void recordCashflow(TransactionType type, Money cashflowAmount, Instant cashflowEventDate, TransactionMetadata transactionMetadata, List<Fee> fees) {
        Objects.requireNonNull(type, "Transaction Type cannot be null.");
        Objects.requireNonNull(cashflowAmount, "Cash flow amount cannot be null.");
        Objects.requireNonNull(cashflowEventDate, "Transaction date cannot be null.");
        Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");

        if (!Set.of(TransactionType.DEPOSIT, TransactionType.WITHDRAWAL, TransactionType.INTEREST, TransactionType.DIVIDEND).contains(type)) {
            throw new IllegalArgumentException("Transaction Type is not DEPOSIT, WITHDRAWAL, INTEREST, OR DIVIDENDS.");
        }

        if (!cashflowAmount.currency().javaCurrency().equals(portfolioCurrencyPreference.javaCurrency())) {
            throw new IllegalArgumentException("You can only record cash flow events with the same currency as your portfolio preference.");
        }

        if (transactionMetadata.transactionStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Transaction Status must be compelted.");
        }

        Money newCashBalance;
        switch (type) {
            case DEPOSIT:
            case INTEREST:
            case DIVIDEND:
                newCashBalance = this.portfolioCashBalance.add(cashflowAmount);
                break;
            case WITHDRAWAL:
                if (this.portfolioCashBalance.amount().compareTo(cashflowAmount.amount()) < 0) {
                    throw new IllegalArgumentException("Insufficient cash balance for withdrawal.");
                }
                newCashBalance = this.portfolioCashBalance.subtract(cashflowAmount); // Assuming Money has a subtract method
                break;
        
            default:
                // backup code, the if clause should have handled this, but incase it doesn't we have this
                assert false : "Unhandled transaction type after validation: " + type;
                throw new IllegalStateException("Unhandled transaction type for cashflow: " + type);
        }

        this.portfolioCashBalance = newCashBalance;

        CashTransactionDetails cashTransactionDetails = new CashTransactionDetails(cashflowAmount);
        UUID transactionId = UUID.randomUUID();

        // Build the Transaction object
        Transaction newTransaction = new Transaction.Builder()
            .transactionId(transactionId)
            .portfolioId(this.portfolioId) // Use the portfolio's own ID
            .transactionType(type)
            .totalTransactionAmount(cashflowAmount) // The amount of this specific cashflow
            .transactionDate(cashflowEventDate)
            .transactionDetails(cashTransactionDetails)
            .transactionMetadata(transactionMetadata)
            .fees(fees != null ? fees: Collections.emptyList()) 
            .build();

        this.transactions.add(newTransaction);

        // Update the portfolio's last modified timestamp
        this.updatedAt = Instant.now();
 
    }
    
    // NOTE: will need to handle the currency conversion of stuff so if bought in USD, we need to update our currencyPerference
    // also need ot deduct from teh cashBalance, hence the reason for the conversion
    // we will need some sort of service to do the conversion
    public AssetHolding recordAssetHoldingPurchase(AssetIdentifier assetIdentifier, BigDecimal quantityOfAssetBought, Instant acquisitionDate, Money pricePerUnit, TransactionMetadata transactionMetadata, List<Fee> fees) {
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
        Objects.requireNonNull(quantityOfAssetBought, "Quantity of asset bought cannot be null.");
        Objects.requireNonNull(acquisitionDate, "Acquisition date cannot be null.");
        Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null.");
        Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");

        if (quantityOfAssetBought.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity cannot be zero or negative.");
            
        }

        if (pricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price Per Unit cannot be zero or negative.");
        }

        if (!Set.of(TransactionStatus.ACTIVE, TransactionStatus.PENDING, TransactionStatus.COMPLETED).contains(transactionMetadata.transactionStatus())) {
            throw new IllegalArgumentException();
        }

        //should fees also have a check? it should probably just the the same as the asset

        fees = fees != null ? fees : Collections.emptyList();
        Money totalFees = new Money(BigDecimal.ZERO, portfolioCurrencyPreference);
        if (fees != null) {
            double tFees = 0D;
            for (Fee fee : fees) {
                tFees += fee.amount().amount().doubleValue();
            }
            totalFees = new Money(new BigDecimal(tFees), pricePerUnit.currency());
        }

        
        Optional<AssetHolding> existingHolding = this.assetHoldings.stream()
        .filter((ah -> ah.getAssetIdentifier().equals(assetIdentifier)))
        .findFirst();
        
        // AssetHolding -> no fees, just deals with the holding or APPL or GOOGL
        Money totalPurchaseCost = pricePerUnit.multiply(quantityOfAssetBought).subtract(totalFees); // Calculate total cost once
        
        AssetHolding holding;
        if (existingHolding.isPresent()) {
            holding = existingHolding.get();
            holding.recordAdditionPurchaseOfAssetHolding(quantityOfAssetBought, totalPurchaseCost);
        }
        else {
            UUID assHoldingId = UUID.randomUUID();
            holding = new AssetHolding(assHoldingId, assHoldingId, assetIdentifier, quantityOfAssetBought, acquisitionDate.atZone(ZoneId.systemDefault()), totalPurchaseCost);
            this.assetHoldings.add(holding);
        }
        
        
        UUID transactionId = UUID.randomUUID();
        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, quantityOfAssetBought, pricePerUnit);
        
        Transaction newTransaction = new Transaction.Builder()
            .transactionId(transactionId)
            .portfolioId(this.portfolioId) // Use the portfolio's own ID
            .transactionType(TransactionType.BUY)
            .totalTransactionAmount(pricePerUnit) // FIX THIS
            .transactionDate(acquisitionDate)
            .transactionDetails(assetTransactionDetails)
            .transactionMetadata(transactionMetadata)
            .fees(fees)
            .build();


        this.transactions.add(newTransaction);
        this.updatedAt = Instant.now();    

        return holding;
        
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
