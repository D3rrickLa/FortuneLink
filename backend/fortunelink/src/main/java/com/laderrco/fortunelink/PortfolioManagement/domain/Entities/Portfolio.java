package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Services.ITransactionCorrectionService;
import com.laderrco.fortunelink.PortfolioManagement.domain.Services.TransactionCorrectionService;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
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
    private PortfolioCurrency currencyPreference;

    private List<AssetHolding> assetHoldings;
    private List<Liability> liabilities;
    private List<Transaction> transactions;

    private Instant createdAt;
    private Instant updatedAt;

    private final ITransactionCorrectionService transactionCorrectionService; 

    public Portfolio(UUID portfolioId, UUID userId, String portfolioName, String portfolioDescription,
            PortfolioCurrency currencyPref,
            boolean isPrimary) {
        if (portfolioId == null) {
            throw new IllegalArgumentException("Portfolio must have an ID assigned to it.");

        }
        if (userId == null) {
            throw new IllegalArgumentException("Portfolio must have a User assigned to it.");
        }

        if (portfolioName == null || portfolioName.trim().isEmpty()) {
            throw new IllegalArgumentException("Portfolio must be given a name.");
        }

        if (currencyPref == null) {
            throw new IllegalArgumentException("Portfolio must have a currency preference.");
        }

        this.userId = userId;
        this.name = portfolioName;
        this.description = portfolioDescription;
        this.currencyPreference = currencyPref;

        this.isPrimary = isPrimary;

        this.portfolioId = portfolioId;

        this.assetHoldings = new ArrayList<>();
        this.liabilities = new ArrayList<>();
        this.transactions = new ArrayList<>();

        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        this.transactionCorrectionService = new TransactionCorrectionService();
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
    // AI assisted this method
    public AssetHolding recordAssetPurchase(AssetIdentifier assetIdentifer, BigDecimal quantity,
            Money costBasisPerUnit, LocalDate acquisitionDate, Money currenyMarketPrice) {

        Objects.requireNonNull(assetIdentifer, "Asset Identifier cannot be null.");
        Objects.requireNonNull(quantity, "Quantity cannot be null.");
        Objects.requireNonNull(costBasisPerUnit, "Cost basis per unit cannot be null.");
        Objects.requireNonNull(acquisitionDate, "Acquisition date cannot be null.");

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Purchase quantity must be positive.");
        }

        if (!this.currencyPreference.code().equals(costBasisPerUnit.currencyCode().code().toString())) {
            throw new IllegalArgumentException("Purchase currency mismatch with portfolio currency preference.");
        }

        Optional<AssetHolding> existingHolding = assetHoldings.stream()
                .filter(ah -> ah.getAssetIdentifier().equals(assetIdentifer))
                .findFirst();

        AssetHolding holding;
        Money totalPurchaseCost = costBasisPerUnit.multiply(quantity); // Calculate total cost once

        if (existingHolding.isPresent()) {
            holding = existingHolding.get();
            holding.recordAdditionalPurchase(quantity, totalPurchaseCost);
        } else {
            holding = new AssetHolding(UUID.randomUUID(), this.portfolioId, assetIdentifer, quantity, totalPurchaseCost,
                    acquisitionDate);
            this.assetHoldings.add(holding);
        }

        // Simultaneously record the transaction within the same aggregate boundary
        Transaction newTransaction = new Transaction(
                UUID.randomUUID(),
                this.portfolioId,
                TransactionType.BUY,
                totalPurchaseCost,
                Instant.now(),
                "Buy " + quantity + " of " + assetIdentifer.toCanonicalString(),
                quantity,
                costBasisPerUnit.amount(),
                holding.getAssetHoldingId(),
                null

        );
        this.transactions.add(newTransaction);

        this.updatedAt = Instant.now(); // Update the aggregate root's timestamp
        return holding;
    }

    // AI assisted with this method
    public void recordAssetSale(AssetIdentifier assetIdentifier, BigDecimal quantityToSell, Money salePricePerUnit,
            Instant transactionDate) {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        Objects.requireNonNull(quantityToSell, "Quantity to sell cannot be null.");
        Objects.requireNonNull(salePricePerUnit, "Sale price cannot be null.");

        if (quantityToSell.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to sell can't be less than 0.");
        }
        // Ensure the sale currency matches the portfolio's preferred currency
        // ACTION REQURIED - portoflio with diff currency??
        if (!this.currencyPreference.code().equals(salePricePerUnit.currencyCode().code())) {
            throw new IllegalArgumentException("Sale currency mismatch with portfolio currency preference.");
        }

        // Finding the relevant AssetHolding
        Optional<AssetHolding> foundHolding = this.assetHoldings.stream()
                .filter(ah -> ah.getAssetIdentifier().equals(assetIdentifier))
                .findFirst();

        if (foundHolding.isEmpty()) {
            throw new IllegalArgumentException("Asset holding with identifier " + assetIdentifier.toCanonicalString()
                    + " not found in portfolio.");
        }

        AssetHolding holding = foundHolding.get();

        // Aggregate-level quantity validation (pre-check before delegation)
        if (quantityToSell.compareTo(holding.getQuantity()) > 0) {
            throw new IllegalArgumentException("Cannot sell " + quantityToSell + " units. Only " + holding.getQuantity()
                    + " available for " + assetIdentifier.toCanonicalString() + " (Asset Holding ID: "
                    + holding.getAssetHoldingId() + ").");
        }

        // delegate sale logic to the AssetHolding
        holding.recordSale(quantityToSell, salePricePerUnit);

        // record transaction
        Money totalSaleProceeds = salePricePerUnit.multiply(quantityToSell);

        Transaction saleTransaction = new Transaction(
                UUID.randomUUID(),
                this.portfolioId,
                TransactionType.SELL,
                totalSaleProceeds,
                transactionDate,
                "Sale of " + quantityToSell + " of " + assetIdentifier.toCanonicalString(),
                quantityToSell,
                salePricePerUnit.amount(),
                holding.getAssetHoldingId(),
                null

        );

        this.transactions.add(saleTransaction);

        // remove AssetHolding if its quantity drop to 0
        if (holding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            this.assetHoldings.remove(holding);
            // Note: Transactions related to this AssetHolding remain with the Portfolio.
            // The AssetHolding itself is just no longer actively held.
        }

        // Update Portfolio's timestamp
        this.updatedAt = Instant.now();
    }

    // Liability Management (i.e. Debt)
    public Liability addLiability(String name, String description, Money initialAmount, Percentage interestRate,
            LocalDate maturityDate) {
        Objects.requireNonNull(name, "Liability name cannot be null.");
        Objects.requireNonNull(initialAmount, "Initial amount owned cannot be null.");
        Objects.requireNonNull(interestRate, "Interest rate cannot be null.");
        Objects.requireNonNull(maturityDate, "Date when liability is due cannot be null.");

        if (initialAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial amount for liability must be positive.");

        }

        if (interestRate.value().compareTo(BigDecimal.ZERO) < 0) { // some interest rates can be 0
            throw new IllegalArgumentException("Interest rate must be positive.");
        }

        // Handle description: Make it non-null explicitly, then check for blank
        // Better to ensure 'description' is not null first if it's meant to be optional
        // but non-null
        String finalDescription = (description == null || description.isBlank()) ? "Liability for " + name
                : description;

        Liability newLiability = new Liability(
                UUID.randomUUID(),
                this.portfolioId,
                name,
                finalDescription,
                initialAmount,
                interestRate,
                maturityDate);
        this.liabilities.add(newLiability);

        this.updatedAt = Instant.now(); // Update the aggregate root's timestamp
        return newLiability;
    }
    
    // AI assisted
    public void recordLiabilityPayment(UUID liabilityId, Money paymentAmount, Instant transactionDate) {
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");

        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }

        // Ensure payment currency matches portfolio's preferred currency
        if (!this.currencyPreference.code().equals(paymentAmount.currencyCode().code())) {
            throw new IllegalArgumentException("Payment currency mismatch with portfolio currency preference.");
        }

        Optional<Liability> optionalLiability = this.liabilities.stream()
                .filter(l -> l.getLiabilityId().equals(liabilityId))
                .findFirst();

        if (optionalLiability.isEmpty()) {
            throw new IllegalArgumentException("Liability with ID " + liabilityId + " not found in portfolio.");
        }

        Liability liability = optionalLiability.get();

        // 1. Delegate payment handling to the Liability entity
        liability.makePayment(paymentAmount); // This will update liability.currentBalance

        // 2. Create and record a Transaction for the payment
        Transaction paymentTransaction = new Transaction(
                UUID.randomUUID(),
                this.portfolioId,
                TransactionType.LOAN_PAYMENT, // Use a specific type for loan payments
                paymentAmount,
                transactionDate, // Use the provided transactionDate
                "Payment for liability '" + liability.getName() + "' (ID: " + liability.getLiabilityId() + ")",
                null,
                null,
                null, // No asset holding associated with this payment
                liabilityId // This transaction is linked to this liability
        );
        this.transactions.add(paymentTransaction);

        // 3. Update Portfolio's timestamp
        this.updatedAt = Instant.now();
    }

    // AI assisted
    // NOTE: the reason we have this method and not a removeAsset is because
    // we want to remove this once we paid off the liability. if we 'void' it, we
    // are not
    // getting closer to the goal
    public void removeLiability(UUID liabilityId) {
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");

        Optional<Liability> optionalLiability = this.liabilities.stream()
                .filter(l -> l.getLiabilityId().equals(liabilityId))
                .findFirst();

        if (optionalLiability.isEmpty()) {
            throw new IllegalArgumentException("Liability with ID " + liabilityId + " not found in portfolio.");
        }

        Liability liabilityToRemove = optionalLiability.get();

        // Prevent removal if there's an outstanding balance
        if (liabilityToRemove.getCurrentBalance().amount().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot remove liability '" + liabilityToRemove.getName() + "' (ID: "
                    + liabilityId + ") with an outstanding balance of " + liabilityToRemove.getCurrentBalance() + ".");
        }

        // Remove the liability from the collection
        this.liabilities.remove(liabilityToRemove);

        this.updatedAt = Instant.now(); // Update the aggregate root's timestamp
    }

    // Transaction Management (for non-asset/liability related, or voiding existing
    // ones)
    public void recordCashTransaction(TransactionType type, Money amount, String description, Instant transactionDate) {
        Objects.requireNonNull(type, "Transaction type for cannot be null.");
        Objects.requireNonNull(amount, "Transfer amount cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");

        if (!this.currencyPreference.code().equals(amount.currencyCode().code())) {
            throw new IllegalArgumentException("Transaction currency mismatch with portfolio currency preference.");
        }

        // Additional validation specific to this method's purpose:
        // Ensure the provided type is actually a 'cash-only' type as per Transaction's
        // rules.
        boolean isCashOnlyType = (type == TransactionType.DEPOSIT ||
                type == TransactionType.WITHDRAWAL ||
                type == TransactionType.DIVIDEND ||
                type == TransactionType.INTEREST_INCOME);

        if (!isCashOnlyType) {
            throw new IllegalArgumentException(
                    "Invalid transaction type for cash transaction. Must be DEPOSIT, WITHDRAWAL, DIVIDEND, or INTEREST_INCOME.");
        }

        Transaction paymentTransaction = new Transaction(
                UUID.randomUUID(),
                this.portfolioId,
                type, // Use a specific type for loan payments
                amount,
                transactionDate, // Use the provided transactionDate
                description,
                null,
                null,
                null, // No asset holding associated with this
                null // No asset liability associated with this
        );
        this.transactions.add(paymentTransaction);

        // 3. Update Portfolio's timestamp
        this.updatedAt = Instant.now();

    }

    // NOTE: for updating an asset, we can't have an update method, not good if you
    // could change your Robinhood transaction
    // instead we 'VOID' the transaction by updating the status. So if we mis-input
    // a 'buy', we void-sell it
    public void voidTransaction(UUID transactionId, String reason) {
        Objects.requireNonNull(transactionId, "Transaction ID to void cannot be null.");
        Objects.requireNonNull(reason, "Reason for voiding cannot be null.");

        Optional<Transaction> optionalTransaction = this.transactions.stream()
                .filter(t -> t.getTransactionId().equals(transactionId))
                .findFirst();

        if (optionalTransaction.isEmpty()) {
            throw new IllegalStateException("Transaction with ID " + transactionId + " cannot be found in portfolio.");
        }

        Transaction transactionToVoid = optionalTransaction.get();

        this.transactionCorrectionService.applyCompensationForVoidedTransaction(this, transactionToVoid, reason);

        transactionToVoid.markAsVoided(reason);

        this.updatedAt = Instant.now(); // Update portfolio timestamp
    }

    // helper method for the transaction service to add voided stuff
    public void addInternalTransaction(Transaction transaction) {
        this.transactions.add(transaction);
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
        return Objects.hash(portfolioId);
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
        return currencyPreference;
    }

    public List<AssetHolding> getAssets() {
        return assetHoldings;
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
