package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.FeeType;
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

        if (!Set.of(TransactionType.DEPOSIT, TransactionType.WITHDRAWAL, TransactionType.INTEREST, TransactionType.DIVIDEND, TransactionType.EXPENSE, TransactionType.FEE).contains(type)) {
            throw new IllegalArgumentException("Transaction Type must be either DEPOSIT, WITHDRAWAL, INTEREST, EXPENSE, FEE, or DIVIDEND.");
        }

        if (cashflowAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cash amount for " + type + " cannot less than or equal to zero.");
        }

        if (!transactionMetadata.transactionStatus().equals(TransactionStatus.COMPLETED)) {
            throw new IllegalArgumentException("Status in metadata must be COMPLETED.");
        }

        // checking to see if cashflowAmount is in native currency 
        Money convertedCashflowAmount;
        BigDecimal exchangeRate; // exhange rate one the cash amount itself (exclu. fees)
        if (cashflowAmount.currency().javaCurrency().equals(this.portfolioCurrencyPreference.javaCurrency())) {
            convertedCashflowAmount = cashflowAmount;
            exchangeRate = BigDecimal.ONE;
        }
        else {
            exchangeRate = exchangeRateService.getCurrencyExchangeRate(cashflowAmount.currency().javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(), cashflowEventDate);
            convertedCashflowAmount = cashflowAmount.convert(this.portfolioCurrencyPreference.javaCurrency(), exchangeRate, RoundingMode.HALF_EVEN);
        }

        // checking to see if fees are in native currency
        Money totalOtherFeesInPortoflioCurrency = Money.ZERO(this.portfolioCurrencyPreference);
        Money totalFOREXConversionFeesInPortfolioCurrency = Money.ZERO(this.portfolioCurrencyPreference);
        fees = fees != null ? fees : Collections.emptyList();
        for (Fee fee : fees) {

            Money feeAmountInPortfolioCurrency;
            if (fee.amount().currency().javaCurrency().equals(this.portfolioCurrencyPreference.javaCurrency())) {
                feeAmountInPortfolioCurrency = fee.amount();
            }  
            else {
                // the forex service stuff here
                BigDecimal feeExchangeRate = exchangeRateService.getCurrencyExchangeRate(fee.amount().currency().javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(), cashflowEventDate);
                feeAmountInPortfolioCurrency = fee.amount().convert(this.portfolioCurrencyPreference.javaCurrency(), feeExchangeRate, RoundingMode.HALF_EVEN);
            } 


            // we divided the logic into forex fees and other fees, this is the check
            if (fee.feeType() == FeeType.FOREIGN_EXCHANGE_CONVERSION) {
                totalFOREXConversionFeesInPortfolioCurrency = totalFOREXConversionFeesInPortfolioCurrency.add(feeAmountInPortfolioCurrency);
            }
            else {
                totalOtherFeesInPortoflioCurrency = totalOtherFeesInPortoflioCurrency.add(feeAmountInPortfolioCurrency);
            }
        }

        // calculating new balance after fees
        Money netPortfolioCashImpact;
        if (Set.of(TransactionType.DEPOSIT, TransactionType.INTEREST, TransactionType.DIVIDEND).contains(type)) {
            netPortfolioCashImpact = convertedCashflowAmount.subtract(totalOtherFeesInPortoflioCurrency).subtract(totalFOREXConversionFeesInPortfolioCurrency);
        }
        else {
            // TransactionType.WITHDRAWAL
            Money totalWithdrawalAmount = convertedCashflowAmount.add(totalOtherFeesInPortoflioCurrency).add(totalFOREXConversionFeesInPortfolioCurrency);
            if (this.portfolioCashBalance.compareTo(totalWithdrawalAmount) < 0) {
                throw new IllegalArgumentException("Cash withdrawal is larger than what you have in this portfolio.");
            }
            netPortfolioCashImpact = totalWithdrawalAmount.negate();
        }

        this.portfolioCashBalance = this.portfolioCashBalance.add(netPortfolioCashImpact);

        Transaction newCashTransaction = TransactionFactory.createCashTransaction(
            UUID.randomUUID(), 
            this.portfolioId, 
            type, 
            cashflowEventDate, 
            cashflowAmount, 
            convertedCashflowAmount, 
            exchangeRate, 
            totalFOREXConversionFeesInPortfolioCurrency, 
            totalFOREXConversionFeesInPortfolioCurrency, 
            netPortfolioCashImpact, 
            transactionMetadata, 
            fees);

        this.transactions.add(newCashTransaction);

        this.updatedAt = Instant.now();
    }
    
    
    /**
     * 
     * @param assetIdentifier
     * @param quantityOfAssetBought
     * @param acquisitionDate
     * @param rawPricePerUnit - pruse price of the asset in its native currency
     * @param transactionMetadata
     * @param fees
     * @return
     */
    public AssetHolding recordAssetHoldingPurchase(AssetIdentifier assetIdentifier, BigDecimal quantityOfAssetBought, Instant acquisitionDate, Money rawPricePerUnit, TransactionMetadata transactionMetadata, List<Fee> fees) {
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null");
        Objects.requireNonNull(quantityOfAssetBought, "Quantity cannot be null.");
        Objects.requireNonNull(acquisitionDate, "Acquisition date cannot be null.");
        Objects.requireNonNull(rawPricePerUnit, "Price per unit cannot be null.");
        Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");
        
        if (quantityOfAssetBought.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Purchase quantity must be greater than zero.");
        }
        
        if (rawPricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price per unit must be greater than zero.");
        }
        
        if (!transactionMetadata.transactionStatus().equals(TransactionStatus.COMPLETED)) {
            throw new IllegalArgumentException("Transaction status must be COMPLETED for a new asset purchase.");
        }

        Money grossAssetCostInAssetCurrency = rawPricePerUnit.multiply(quantityOfAssetBought);
        PortfolioCurrency assetNativeCurrency = new PortfolioCurrency(rawPricePerUnit.currency().javaCurrency());

        Money feesAddedToCostBasisInAssetCurrency = Money.ZERO(assetNativeCurrency); // for summing all cashflow reporting
        Money totalOtherFeesInPortoflioCurrency = Money.ZERO(this.portfolioCurrencyPreference);
        Money totalFOREXConversionFeesInPortfolioCurrency = Money.ZERO(this.portfolioCurrencyPreference);
       
        fees = fees != null ? fees : Collections.emptyList();

        // goal for fee loop -> process each individual fee object from hte inputted list and to
        // For Asset Cost Basis: determine if the fee should be added to the assetholding's cost basis, if so conver it to the asset's native currency and add it to 'feesAddedTOCostBasisInAssetCurrency'
        // for cashflow reporting: categorize the fees and vonert it to the portfolio's native currency
        for (Fee fee : fees) {
            Objects.requireNonNull(fee, "Fee cannot be null.");
            Objects.requireNonNull(fee.amount(), "Fee amount cannot be null.");
            Objects.requireNonNull(fee.feeType(), "Fee type cannot be null.");

            if (fee.amount().amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Fee amount cannot be negative for " + fee.feeType());
            }

            // --- A. Handling Fees for ASSET COST BASIS ---
            // Goal: Figure out how much of this 'fee' should increase the asset's cost basis.
            // The cost basis must always be in the ASSET'S NATIVE CURRENCY (e.g., USD for AAPL).

            // Condition: Is this fee type one that should add to the asset's cost basis?
            // Your policy: BROKERAGE or REGULATORY fees add to cost basis.
            if (fee.feeType() == FeeType.BROKERAGE || fee.feeType() == FeeType.REGULATORY) {
                Money feeForCostBasis; 
                if (fee.amount().currency().javaCurrency().equals(assetNativeCurrency.javaCurrency())) {
                    feeForCostBasis = fee.amount();
                }
                else { // if the fee is in a different currency, must convert to asset's native currency
                    BigDecimal feeToAssetRate = exchangeRateService.getCurrencyExchangeRate(fee.amount().currency().javaCurrency(), assetNativeCurrency.javaCurrency(), acquisitionDate);
                    feeForCostBasis = fee.amount().convert(assetNativeCurrency.javaCurrency(), feeToAssetRate, RoundingMode.HALF_EVEN);
                }

                feesAddedToCostBasisInAssetCurrency = feesAddedToCostBasisInAssetCurrency.add(feeForCostBasis);
            }

             // --- B. Handling Fees for CASHFLOW REPORTING ---
            // Goal: Figure out how much this 'fee' impacts the portfolio's cash balance.
            // The cash balance and its impact must always be in the PORTFOLIO'S NATIVE CURRENCY (e.g., CAD).
            Money feeForCashflowReporting;
            if (fee.amount().currency().javaCurrency().equals(this.portfolioCurrencyPreference.javaCurrency())) {
                feeForCashflowReporting = fee.amount();
            }
            else {
                BigDecimal feeToPortfolioRate = exchangeRateService.getCurrencyExchangeRate(fee.amount().currency().javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(), acquisitionDate);
                feeForCashflowReporting = fee.amount().convert(this.portfolioCurrencyPreference.javaCurrency(), feeToPortfolioRate, RoundingMode.HALF_EVEN);
            }

            if (fee.feeType() == FeeType.FOREIGN_EXCHANGE_CONVERSION) {
                totalFOREXConversionFeesInPortfolioCurrency = totalFOREXConversionFeesInPortfolioCurrency.add(feeForCashflowReporting);
            }
            else {
                totalOtherFeesInPortoflioCurrency = totalOtherFeesInPortoflioCurrency.add(feeForCashflowReporting);
            }
        }

            
            
        Money totalAssetCostBasisInAssetCurrency = grossAssetCostInAssetCurrency.add(feesAddedToCostBasisInAssetCurrency);
        Optional<AssetHolding> existingHolding = assetHoldings.stream()
                .filter(ah -> ah.getAssetIdentifier().equals(assetIdentifier))
                .findFirst();

        AssetHolding holding;
        if (existingHolding.isPresent()) {
            holding = existingHolding.get();
            holding.recordAdditionPurchaseOfAssetHolding(quantityOfAssetBought, totalAssetCostBasisInAssetCurrency);
        }
        else {
            holding = new AssetHolding(UUID.randomUUID(), portfolioId, assetIdentifier, quantityOfAssetBought, acquisitionDate, totalAssetCostBasisInAssetCurrency);
            this.assetHoldings.add(holding);
        }

        // calcualting the net cash impact on the portfolio
        BigDecimal assetCostToPorfolioRate = exchangeRateService.getCurrencyExchangeRate(assetNativeCurrency.javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(), acquisitionDate);
        Money grossAssestCostInPorfolioCurrency = grossAssetCostInAssetCurrency.convert(this.portfolioCurrencyPreference.javaCurrency(), assetCostToPorfolioRate, RoundingMode.HALF_EVEN);

        Money netPortfolioCashImpact = grossAssestCostInPorfolioCurrency.add(totalFOREXConversionFeesInPortfolioCurrency).add(totalOtherFeesInPortoflioCurrency).negate();

        if (this.portfolioCashBalance.amount().compareTo(netPortfolioCashImpact.amount().abs()) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance to complete asset purchase. Required: " + netPortfolioCashImpact + ", Available: " + this.portfolioCashBalance);
        }

        this.portfolioCashBalance = this.portfolioCashBalance.add(netPortfolioCashImpact);


        // need same treatment from cashflow with the new stuff
        Transaction newAssetTransaction = TransactionFactory.createBuyAssetTransaction(
            UUID.randomUUID(), 
            this.portfolioId, 
            assetIdentifier, 
            acquisitionDate, 
            quantityOfAssetBought, 
            rawPricePerUnit, 
            grossAssetCostInAssetCurrency,
            grossAssestCostInPorfolioCurrency,
            netPortfolioCashImpact,
            totalFOREXConversionFeesInPortfolioCurrency,
            totalOtherFeesInPortoflioCurrency,
            transactionMetadata, 
            fees)
        ;
        this.transactions.add(newAssetTransaction);
       
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
