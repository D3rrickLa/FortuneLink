package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
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
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.LiabilityPaymentDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ReversalDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionSource;
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

    public void recordCashflow(TransactionType type, Money cashflowAmount, Instant cashflowEventDate,
            TransactionMetadata transactionMetadata, List<Fee> fees) {
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

        if (!Set.of(TransactionType.DEPOSIT, TransactionType.WITHDRAWAL, TransactionType.INTEREST,
                TransactionType.DIVIDEND, TransactionType.EXPENSE, TransactionType.FEE).contains(type)) {
            throw new IllegalArgumentException(
                    "Transaction Type must be either DEPOSIT, WITHDRAWAL, INTEREST, EXPENSE, FEE, or DIVIDEND.");
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
        } else {
            exchangeRate = exchangeRateService.getCurrencyExchangeRate(cashflowAmount.currency().javaCurrency(),
                    this.portfolioCurrencyPreference.javaCurrency(), cashflowEventDate);
            convertedCashflowAmount = cashflowAmount.convert(this.portfolioCurrencyPreference.javaCurrency(),
                    exchangeRate, RoundingMode.HALF_EVEN);
        }

        // checking to see if fees are in native currency
        Money totalOtherFeesInPortoflioCurrency = Money.ZERO(this.portfolioCurrencyPreference);
        Money totalFOREXConversionFeesInPortfolioCurrency = Money.ZERO(this.portfolioCurrencyPreference);
        fees = fees != null ? fees : Collections.emptyList();
        for (Fee fee : fees) {

            Money feeAmountInPortfolioCurrency;
            if (fee.amount().currency().javaCurrency().equals(this.portfolioCurrencyPreference.javaCurrency())) {
                feeAmountInPortfolioCurrency = fee.amount();
            } else {
                // the forex service stuff here
                BigDecimal feeExchangeRate = exchangeRateService.getCurrencyExchangeRate(
                        fee.amount().currency().javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(),
                        cashflowEventDate);
                feeAmountInPortfolioCurrency = fee.amount().convert(this.portfolioCurrencyPreference.javaCurrency(),
                        feeExchangeRate, RoundingMode.HALF_EVEN);
            }

            // we divided the logic into forex fees and other fees, this is the check
            if (fee.feeType() == FeeType.FOREIGN_EXCHANGE_CONVERSION) {
                totalFOREXConversionFeesInPortfolioCurrency = totalFOREXConversionFeesInPortfolioCurrency
                        .add(feeAmountInPortfolioCurrency);
            } else {
                totalOtherFeesInPortoflioCurrency = totalOtherFeesInPortoflioCurrency.add(feeAmountInPortfolioCurrency);
            }
        }

        // calculating new balance after fees
        Money netPortfolioCashImpact;
        if (Set.of(TransactionType.DEPOSIT, TransactionType.INTEREST, TransactionType.DIVIDEND).contains(type)) {
            netPortfolioCashImpact = convertedCashflowAmount.subtract(totalOtherFeesInPortoflioCurrency)
                    .subtract(totalFOREXConversionFeesInPortfolioCurrency);
        } else {
            // TransactionType.WITHDRAWAL
            Money totalWithdrawalAmount = convertedCashflowAmount.add(totalOtherFeesInPortoflioCurrency)
                    .add(totalFOREXConversionFeesInPortfolioCurrency);
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
     * @param rawPricePerUnit       - pruse price of the asset in its native
     *                              currency
     * @param transactionMetadata
     * @param fees
     * @return
     */
    public AssetHolding recordAssetHoldingPurchase(AssetIdentifier assetIdentifier, BigDecimal quantityOfAssetBought,
            Instant acquisitionDate, Money rawPricePerUnit, TransactionMetadata transactionMetadata, List<Fee> fees) {
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

        Money feesAddedToCostBasisInAssetCurrency = Money.ZERO(assetNativeCurrency); // for summing all cashflow
                                                                                     // reporting
        Money totalOtherFeesInPortoflioCurrency = Money.ZERO(this.portfolioCurrencyPreference);
        Money totalFOREXConversionFeesInPortfolioCurrency = Money.ZERO(this.portfolioCurrencyPreference);

        fees = fees != null ? fees : Collections.emptyList();

        // goal for fee loop -> process each individual fee object from hte inputted
        // list and to
        // For Asset Cost Basis: determine if the fee should be added to the
        // assetholding's cost basis, if so conver it to the asset's native currency and
        // add it to 'feesAddedTOCostBasisInAssetCurrency'
        // for cashflow reporting: categorize the fees and vonert it to the portfolio's
        // native currency
        for (Fee fee : fees) {
            Objects.requireNonNull(fee, "Fee cannot be null.");
            Objects.requireNonNull(fee.amount(), "Fee amount cannot be null.");
            Objects.requireNonNull(fee.feeType(), "Fee type cannot be null.");

            if (fee.amount().amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Fee amount cannot be negative for " + fee.feeType());
            }

            // --- A. Handling Fees for ASSET COST BASIS ---
            // Goal: Figure out how much of this 'fee' should increase the asset's cost
            // basis.
            // The cost basis must always be in the ASSET'S NATIVE CURRENCY (e.g., USD for
            // AAPL).

            // Condition: Is this fee type one that should add to the asset's cost basis?
            // Your policy: BROKERAGE or REGULATORY fees add to cost basis.
            if (fee.feeType() == FeeType.BROKERAGE || fee.feeType() == FeeType.REGULATORY) {
                Money feeForCostBasis;
                if (fee.amount().currency().javaCurrency().equals(assetNativeCurrency.javaCurrency())) {
                    feeForCostBasis = fee.amount();
                } else { // if the fee is in a different currency, must convert to asset's native
                         // currency
                    BigDecimal feeToAssetRate = exchangeRateService.getCurrencyExchangeRate(
                            fee.amount().currency().javaCurrency(), assetNativeCurrency.javaCurrency(),
                            acquisitionDate);
                    feeForCostBasis = fee.amount().convert(assetNativeCurrency.javaCurrency(), feeToAssetRate,
                            RoundingMode.HALF_EVEN);
                }

                feesAddedToCostBasisInAssetCurrency = feesAddedToCostBasisInAssetCurrency.add(feeForCostBasis);
            }

            // --- B. Handling Fees for CASHFLOW REPORTING ---
            // Goal: Figure out how much this 'fee' impacts the portfolio's cash balance.
            // The cash balance and its impact must always be in the PORTFOLIO'S NATIVE
            // CURRENCY (e.g., CAD).
            Money feeForCashflowReporting;
            if (fee.amount().currency().javaCurrency().equals(this.portfolioCurrencyPreference.javaCurrency())) {
                feeForCashflowReporting = fee.amount();
            } else {
                BigDecimal feeToPortfolioRate = exchangeRateService.getCurrencyExchangeRate(
                        fee.amount().currency().javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(),
                        acquisitionDate);
                feeForCashflowReporting = fee.amount().convert(this.portfolioCurrencyPreference.javaCurrency(),
                        feeToPortfolioRate, RoundingMode.HALF_EVEN);
            }

            if (fee.feeType() == FeeType.FOREIGN_EXCHANGE_CONVERSION) {
                totalFOREXConversionFeesInPortfolioCurrency = totalFOREXConversionFeesInPortfolioCurrency
                        .add(feeForCashflowReporting);
            } else {
                totalOtherFeesInPortoflioCurrency = totalOtherFeesInPortoflioCurrency.add(feeForCashflowReporting);
            }
        }

        Money totalAssetCostBasisInAssetCurrency = grossAssetCostInAssetCurrency
                .add(feesAddedToCostBasisInAssetCurrency);
        Optional<AssetHolding> existingHolding = assetHoldings.stream()
                .filter(ah -> ah.getAssetIdentifier().equals(assetIdentifier))
                .findFirst();

        AssetHolding holding;
        if (existingHolding.isPresent()) {
            holding = existingHolding.get();
            holding.recordAdditionPurchaseOfAssetHolding(quantityOfAssetBought, totalAssetCostBasisInAssetCurrency);
        } else {
            holding = new AssetHolding(UUID.randomUUID(), portfolioId, assetIdentifier, quantityOfAssetBought,
                    acquisitionDate, totalAssetCostBasisInAssetCurrency);
            this.assetHoldings.add(holding);
        }

        // calculate the net cash impact on the portfolio
        BigDecimal assetCostToPorfolioRate = exchangeRateService.getCurrencyExchangeRate(
                assetNativeCurrency.javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(), acquisitionDate);
        Money grossAssestCostInPorfolioCurrency = grossAssetCostInAssetCurrency.convert(
                this.portfolioCurrencyPreference.javaCurrency(), assetCostToPorfolioRate, RoundingMode.HALF_EVEN);

        Money netPortfolioCashImpact = grossAssestCostInPorfolioCurrency
                .add(totalFOREXConversionFeesInPortfolioCurrency).add(totalOtherFeesInPortoflioCurrency).negate();

        if (this.portfolioCashBalance.amount().compareTo(netPortfolioCashImpact.amount().abs()) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance to complete asset purchase. Required: "
                    + netPortfolioCashImpact + ", Available: " + this.portfolioCashBalance);
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
                fees);
        this.transactions.add(newAssetTransaction);

        return holding;
    }

    public void recordAssetHoldingSale(AssetIdentifier assetIdentifier, BigDecimal quantityToSell, Instant saleDate,
            Money rawSalePricePerUnit, TransactionMetadata transactionMetadata, List<Fee> fees) {
        Objects.requireNonNull(assetIdentifier, "AssetIdentifier cannot be null.");
        Objects.requireNonNull(quantityToSell, "Quantity to sell cannot be null.");
        Objects.requireNonNull(saleDate, "Sale date cannot be null.");
        Objects.requireNonNull(rawSalePricePerUnit, "Price per unit to sell cannot be null.");
        Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");

        if (quantityToSell.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to sell must be positive");
        }

        if (rawSalePricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sale Price Per Unit must be positive");
        }

        if (transactionMetadata.transactionStatus() != TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("Transaction status must be marked as COMPLETED.");
        }

        Money grossProceedsInAssetCurrency = rawSalePricePerUnit.multiply(quantityToSell);
        PortfolioCurrency assetNativeCurrency = new PortfolioCurrency(rawSalePricePerUnit.currency().javaCurrency());

        Money feesReducingProceedsInAssetCurrency = Money.ZERO(assetNativeCurrency); // For Gain/Loss calc
        Money totalFOREXConversionFeesInPortfolioCurrency = Money.ZERO(this.portfolioCurrencyPreference); // For
                                                                                                          // Cashflow
        Money totalOtherFeesInPortfolioCurrency = Money.ZERO(this.portfolioCurrencyPreference); // For Cashflow

        fees = fees != null ? fees : Collections.emptyList();

        for (Fee fee : fees) {
            Objects.requireNonNull(fee, "Fee cannot be null.");
            Objects.requireNonNull(fee.amount(), "Fee amount cannot be null.");
            Objects.requireNonNull(fee.feeType(), "Fee type cannot be null.");

            if (fee.amount().amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Fee amount cannot be negative for " + fee.feeType());
            }
            // --- A. Handling Fees for GAIN/LOSS Calcualtions ---
            // Goal: Determine how much this fee REDUCES the proceeds (or increases the cost
            // of sale)
            // This must be in the ASSET'S NATIVE CURRENCY.
            if (fee.feeType() == FeeType.BROKERAGE || fee.feeType() == FeeType.REGULATORY) {
                Money feeForGainLossCalc;
                if (fee.amount().currency().javaCurrency().equals(assetNativeCurrency.javaCurrency())) {
                    feeForGainLossCalc = fee.amount();
                } else {
                    BigDecimal feeToAssetRate = exchangeRateService.getCurrencyExchangeRate(
                            fee.amount().currency().javaCurrency(), assetNativeCurrency.javaCurrency(), saleDate);
                    feeForGainLossCalc = fee.amount().convert(assetNativeCurrency.javaCurrency(), feeToAssetRate,
                            RoundingMode.HALF_EVEN);
                }
                feesReducingProceedsInAssetCurrency = feesReducingProceedsInAssetCurrency.add(feeForGainLossCalc);
            }

            // --- B. Handle Fees for CASHFLOW REPORTING ---
            // Goal: Determine how much this fee impacts the portfolio's cash balance.
            // This must be in the PORTFOLIO'S NATIVE CURRENCY.
            Money feeForCashflowReporting;
            if (fee.amount().currency().javaCurrency().equals(this.portfolioCurrencyPreference.javaCurrency())) {
                feeForCashflowReporting = fee.amount();
            } else {
                BigDecimal feeToPortfolioRate = exchangeRateService.getCurrencyExchangeRate(
                        fee.amount().currency().javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(),
                        saleDate);
                feeForCashflowReporting = fee.amount().convert(this.portfolioCurrencyPreference.javaCurrency(),
                        feeToPortfolioRate, RoundingMode.HALF_EVEN);
            }

            if (fee.feeType() == FeeType.FOREIGN_EXCHANGE_CONVERSION) {
                totalFOREXConversionFeesInPortfolioCurrency = totalFOREXConversionFeesInPortfolioCurrency
                        .add(feeForCashflowReporting);
            } else {
                totalOtherFeesInPortfolioCurrency = totalOtherFeesInPortfolioCurrency.add(feeForCashflowReporting);
            }
        }

        Money netProceedsForGainLostCalcInAssetCurrency = grossProceedsInAssetCurrency
                .subtract(feesReducingProceedsInAssetCurrency);
        Optional<AssetHolding> existingHolding = this.assetHoldings.stream()
                .filter(eh -> eh.getAssetIdentifier().equals(assetIdentifier))
                .findFirst();
        AssetHolding holding;
        if (existingHolding.isEmpty()) {
            throw new IllegalArgumentException(
                    "Asset holding with identifier " + assetIdentifier.toString() + " not found in portfolio.");
        } else {
            holding = existingHolding.get();
        }

        if (quantityToSell.compareTo(holding.getQuantity()) > 0) {
            throw new IllegalArgumentException("Cannot sell " + quantityToSell + " units. Only " + holding.getQuantity()
                    + " available for " + assetIdentifier.toString() + " (Asset Holding ID: "
                    + holding.getAssetHoldingId() + ").");
        }

        Money gainOrLossInAssetCurrency = holding.recordSaleOfAssetHolding(quantityToSell,
                netProceedsForGainLostCalcInAssetCurrency);

        BigDecimal assetProccedsToPortfolioRate = exchangeRateService.getCurrencyExchangeRate(
                assetNativeCurrency.javaCurrency(), this.portfolioCurrencyPreference.javaCurrency(), saleDate);

        Money grossProceedsInPortfolioCurrency = grossProceedsInAssetCurrency.convert(
                this.portfolioCurrencyPreference.javaCurrency(), assetProccedsToPortfolioRate, RoundingMode.HALF_EVEN);

        Money netPortfolioCashImpact = grossProceedsInPortfolioCurrency
                .subtract(totalFOREXConversionFeesInPortfolioCurrency).subtract(totalOtherFeesInPortfolioCurrency);
        this.portfolioCashBalance = this.portfolioCashBalance.add(netPortfolioCashImpact);

        Transaction newAssetTransaction = TransactionFactory.createSellAssetTransaction(
                UUID.randomUUID(),
                this.portfolioId,
                assetIdentifier,
                saleDate,
                quantityToSell,
                rawSalePricePerUnit,
                gainOrLossInAssetCurrency,
                grossProceedsInPortfolioCurrency,
                netPortfolioCashImpact,
                totalFOREXConversionFeesInPortfolioCurrency,
                totalOtherFeesInPortfolioCurrency,
                transactionMetadata,
                fees);
        this.transactions.add(newAssetTransaction);
    }

    public Liability recordNewLiability(final UUID portfolioId, String liabilityName, String liabilityDescription,
            Money initialOutstandingBalance, Percentage interestRate, ZonedDateTime maturityDate) {
        Objects.requireNonNull(liabilityName, "Liability name cannot be null.");
        Objects.requireNonNull(initialOutstandingBalance, "Initial amount owned cannot be null.");
        Objects.requireNonNull(interestRate, "Interest rate cannot be null.");
        Objects.requireNonNull(maturityDate, "Date when liability is due cannot be null.");

        if (initialOutstandingBalance.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Initial amount for liability must be positive.");

        }

        if (interestRate.percentValue().compareTo(BigDecimal.ZERO) < 0) { // some interest rates can be 0
            throw new IllegalArgumentException("Interest rate must be positive.");
        }
        String finalDescription = (liabilityDescription == null || liabilityDescription.isBlank())
                ? "Liability for " + liabilityName
                : liabilityDescription;
        Liability newLiability = new Liability(UUID.randomUUID(), portfolioId, liabilityName, finalDescription,
                initialOutstandingBalance, interestRate, maturityDate);

        this.liabilities.add(newLiability);
        this.updatedAt = Instant.now(); // Update the aggregate root's timestamp

        return newLiability;
    }

    public void recordLiabilityPayment(final UUID liabilityId, Money paymentAmount,
            Instant transactionDate, TransactionMetadata transactionMetadata, List<Fee> fees) {
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");

        if (paymentAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }

        Optional<Liability> optionalLiability = this.liabilities.stream()
                .filter(l -> l.getLiabilityId().equals(liabilityId))
                .findFirst();

        if (optionalLiability.isEmpty()) {
            throw new IllegalArgumentException("Liability with ID " + liabilityId + " not found in portfolio.");
        }
        Liability foundLiability = optionalLiability.get();

        // --- 2. Calculate payment amount in LIABILITY'S CURRENCY for updating
        // liability balance ---
        Money paymentAmountInLiabilityCurrency = paymentAmount;
        if (!paymentAmount.currency().equals(foundLiability.getCurrentLiabilityBalance().currency())) {
            BigDecimal rate = exchangeRateService.getCurrencyExchangeRate(
                    paymentAmount.currency().javaCurrency(),
                    foundLiability.getCurrentLiabilityBalance().currency().javaCurrency(),
                    transactionDate);
            paymentAmountInLiabilityCurrency = paymentAmount.convert(
                    foundLiability.getCurrentLiabilityBalance().currency().javaCurrency(),
                    rate,
                    RoundingMode.HALF_EVEN);
        }

        // --- 3. Process Fees for Cashflow Impact ---
        // These fees represent cash outflows from the portfolio due to making the
        // payment.
        Money totalFOREXConversionFeesInPortfolioCurrency = Money.ZERO(this.portfolioCurrencyPreference);
        Money totalOtherFeesInPortfolioCurrency = Money.ZERO(this.portfolioCurrencyPreference);

        fees = fees != null ? fees : Collections.emptyList(); // Ensure fees list is not null

        for (Fee fee : fees) {
            Objects.requireNonNull(fee, "Fee cannot be null.");
            Objects.requireNonNull(fee.amount(), "Fee amount cannot be null.");
            Objects.requireNonNull(fee.feeType(), "Fee type cannot be null.");

            if (fee.amount().amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Fee amount cannot be negative for " + fee.feeType());
            }

            Money feeForCashflowReporting;
            if (fee.amount().currency().javaCurrency().equals(this.portfolioCurrencyPreference.javaCurrency())) {
                feeForCashflowReporting = fee.amount();
            } else {
                BigDecimal feeToPortfolioRate = exchangeRateService.getCurrencyExchangeRate(
                        fee.amount().currency().javaCurrency(),
                        this.portfolioCurrencyPreference.javaCurrency(),
                        transactionDate);
                feeForCashflowReporting = fee.amount().convert(
                        this.portfolioCurrencyPreference.javaCurrency(),
                        feeToPortfolioRate,
                        RoundingMode.HALF_EVEN);
            }

            if (fee.feeType() == FeeType.FOREIGN_EXCHANGE_CONVERSION) {
                totalFOREXConversionFeesInPortfolioCurrency = totalFOREXConversionFeesInPortfolioCurrency
                        .add(feeForCashflowReporting);
            } else {
                totalOtherFeesInPortfolioCurrency = totalOtherFeesInPortfolioCurrency.add(feeForCashflowReporting);
            }
        }

        // --- 4. Calculate payment amount in PORTFOLIO'S CASH CURRENCY for cash balance
        // check and deduction ---
        // This is the total cash outflow from the portfolio for the payment *itself*,
        // before any separate fees are added.
        Money actualPaymentCashOutflowInPortfolioCurrency;
        if (!paymentAmount.currency().equals(this.portfolioCashBalance.currency())) {
            BigDecimal rate = exchangeRateService.getCurrencyExchangeRate(
                    paymentAmount.currency().javaCurrency(),
                    this.portfolioCashBalance.currency().javaCurrency(),
                    transactionDate);
            actualPaymentCashOutflowInPortfolioCurrency = paymentAmount.convert(
                    this.portfolioCashBalance.currency().javaCurrency(),
                    rate,
                    RoundingMode.HALF_EVEN);
        } else {
            actualPaymentCashOutflowInPortfolioCurrency = paymentAmount; // No conversion needed if currencies already
                                                                         // match
        }

        // --- 5. Total Cash Outflow from Portfolio (Payment + Fees) ---
        Money totalCashOutflowFromPortfolio = actualPaymentCashOutflowInPortfolioCurrency
                .add(totalFOREXConversionFeesInPortfolioCurrency)
                .add(totalOtherFeesInPortfolioCurrency);

        // --- 6. Validate Payment Amount vs. Outstanding Balance (in liability's
        // currency) ---
        // This check should use paymentAmountInLiabilityCurrency against
        // foundLiability.getCurrentLiabilityBalance()
        if (paymentAmountInLiabilityCurrency.amount()
                .compareTo(foundLiability.getCurrentLiabilityBalance().amount()) > 0) {
            throw new IllegalArgumentException("Payment amount (" + paymentAmountInLiabilityCurrency
                    + ") exceeds outstanding balance (" + foundLiability.getCurrentLiabilityBalance() + ").");
        }

        // --- 7. Check Portfolio's Cash Balance (using total cash outflow in
        // portfolio's currency) ---
        if (this.portfolioCashBalance.amount().compareTo(totalCashOutflowFromPortfolio.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient cash balance to make payment. Required: "
                    + totalCashOutflowFromPortfolio + ", Available: " + this.portfolioCashBalance);
        }

        // --- 8. Update Portfolio's Cash Balance ---
        this.portfolioCashBalance = this.portfolioCashBalance.subtract(totalCashOutflowFromPortfolio);

        // --- 9. Delegate payment to the Liability ---
        foundLiability.makeLiabilityPayment(paymentAmountInLiabilityCurrency);

        // --- 10. Create and record a transaction ---
        Transaction newLiabilityTransaction = TransactionFactory.createLiabilityPaymentTransaction(
                UUID.randomUUID(),
                this.portfolioId,
                liabilityId,
                paymentAmount, // Original payment amount (e.g., $100 USD)
                actualPaymentCashOutflowInPortfolioCurrency, // The amount of the principal payment from portfolio cash
                                                             // (e.g., $135 CAD)
                paymentAmountInLiabilityCurrency, // The amount reducing the liability (e.g., $100 USD on a USD loan)
                transactionDate,
                foundLiability.getCurrentLiabilityBalance(), // The balance of the liability AFTER payment
                transactionMetadata,
                totalFOREXConversionFeesInPortfolioCurrency, // Calculated fees
                totalOtherFeesInPortfolioCurrency, // Calculated fees
                fees // Original list of fees for audit
        );
        this.transactions.add(newLiabilityTransaction);

        // --- 11. Update Portfolio's overall timestamp ---
        this.updatedAt = Instant.now();
    }

   public void voidTransaction(final UUID transactionIdToVoid, String reason) {
       Objects.requireNonNull(transactionIdToVoid, "Transaction ID to void cannot be null.");
        Objects.requireNonNull(reason, "Reason for voiding cannot be null.");

        if (reason.isBlank()) {
            throw new IllegalArgumentException("Reason for voiding cannot be blank.");
        }

        Optional<Transaction> optionalTransaction = this.transactions.stream()
                .filter(t -> t.getTransactionId().equals(transactionIdToVoid))
                .findFirst();

        if (optionalTransaction.isEmpty()) {
            throw new IllegalStateException("Transaction with ID " + transactionIdToVoid + " cannot be found in portfolio.");
        }

        Transaction originalTransaction = optionalTransaction.get();

        // **IMPORTANT:** No change to originalTransaction's status here!
        // This check is to prevent voiding an already conceptually voided transaction,
        // if your system flags it for reporting purposes.
        // If your TransactionStatus enum has a VOIDED state, and you *do* want to prevent
        // double-voiding (even if the original is immutable, you might have a derived view),
        // then your original TransactionFactory might set its status.
        // For a strictly immutable ledger, you wouldn't have TransactionStatus.VOIDED on original.
        // Instead, you'd check if a reversal transaction for this ID already exists.
        // For simplicity for now, let's assume original is just 'COMPLETED' and we add a REVERSAL.

        // Check if a reversal for this transaction already exists (prevents double reversal)
        boolean alreadyReversed = this.transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.REVERSAL) // Assume you add REVERSAL to your enum
                .map(Transaction::getTransactionDetails)
                .filter(d -> d instanceof ReversalDetails)
                .map(d -> (ReversalDetails) d)
                .anyMatch(rd -> rd.getOriginalTransactionId().equals(transactionIdToVoid));

        if (alreadyReversed) {
            throw new IllegalArgumentException("Transaction with ID " + transactionIdToVoid + " has already been reversed.");
        }
        
        // --- PREPARE FOR FINANCIAL REVERSAL ---
        TransactionType originalType = originalTransaction.getTransactionType();
        TransactionDetails originalDetails = originalTransaction.getTransactionDetails();
        Money originalCashImpactAmount = originalTransaction.getTotalTransactionAmount(); // Assumed to be in portfolio's currency

        // --- Perform Actual Reversals of Portfolio State ---
        // These are the operations that modify the portfolio's balances (cash, holdings, liabilities).
        // Each original transaction type has an inverse effect.

        // 1. Reverse Cash Impact
        Money reversalCashFlow; // This will be the amount that affects portfolioCashBalance
        switch (originalType) {
            case CASH_DEPOSIT:
            case ASSET_SALE:
                // Original: Cash INflow. Reversal: Cash OUTflow.
                reversalCashFlow = originalCashImpactAmount.negate(); // Assuming Money.negate() exists or manually make negative for subtraction
                this.portfolioCashBalance = this.portfolioCashBalance.subtract(originalCashImpactAmount);
                break;
            case CASH_WITHDRAWAL:
            case PAYMENT: // Liability Payment
            case ASSET_ADDITION: // Asset Purchase
                // Original: Cash OUTflow. Reversal: Cash INflow.
                reversalCashFlow = originalCashImpactAmount; // Already positive magnitude
                this.portfolioCashBalance = this.portfolioCashBalance.add(originalCashImpactAmount);
                break;
            default:
                throw new UnsupportedOperationException("Reversal of cash impact for transaction type " + originalType + " is not yet supported.");
        }

        // 2. Reverse Asset Holding / Liability Impact (Delegating to entities)
        // These methods should perform the *opposite* action of the original transaction
        switch (originalType) {
                 case ASSET_ADDITION:
                if (!(originalDetails instanceof AssetTransactionDetails)) {
                    throw new IllegalStateException("Transaction " + originalTransaction.getTransactionId() + " of type ASSET_ADDITION has incorrect details type.");
                }
                AssetTransactionDetails assetBuyDetails = (AssetTransactionDetails) originalDetails;

                Optional<AssetHolding> existingHoldingForBuyReversal = this.assetHoldings.stream()
                        .filter(ah -> ah.getAssetIdentifier().equals(assetBuyDetails.getAssetIdentifier()))
                        .findFirst();

                if (existingHoldingForBuyReversal.isPresent()) {
                    AssetHolding holding = existingHoldingForBuyReversal.get();
                    // MAKE SURE YOU ARE PASSING THE PORTFOLIO CURRENCY COST HERE:
                    holding.reverseAddition(assetBuyDetails.getQuantity(), assetBuyDetails.getGrossAssetCostInPortfolio());
                } else {
                    throw new IllegalStateException("Cannot reverse purchase for asset " + assetBuyDetails.getAssetIdentifier() + " as holding no longer exists or cannot be identified correctly for reversal.");
                }
                break;
                case ASSET_SALE:
                if (!(originalDetails instanceof AssetTransactionDetails)) {
                    throw new IllegalStateException("Transaction " + originalTransaction.getTransactionId() + " of type ASSET_SALE has incorrect details type.");
                }
                AssetTransactionDetails assetSaleDetails = (AssetTransactionDetails) originalDetails;

                Optional<AssetHolding> existingHoldingForSaleReversal = this.assetHoldings.stream()
                        .filter(ah -> ah.getAssetIdentifier().equals(assetSaleDetails.getAssetIdentifier()))
                        .findFirst();

                if (existingHoldingForSaleReversal.isPresent()) {
                    AssetHolding holding = existingHoldingForSaleReversal.get();
                    // CORRECTED LINE: Using getNetProceedsInAssetCurrency()
                    holding.reverseSale(assetSaleDetails.getQuantity(), assetSaleDetails.getNetProceedsInAssetCurrency());
                } else {
                    // This scenario means the asset was entirely sold and removed from holdings.
                    // To "un-sell" it perfectly, you'd need to re-create the AssetHolding with its
                    // original cost basis, which is typically derived from prior purchase transactions
                    // rather than directly stored in the sale details themselves.
                    // This is why full voiding can be very complex.
                    throw new IllegalStateException("Cannot reverse sale for asset " + assetSaleDetails.getAssetIdentifier() + " as holding cannot be identified or recreated for reversal.");
                }
                break;

            case PAYMENT: // Liability Payment
                if (!(originalDetails instanceof LiabilityPaymentDetails)) {
                    throw new IllegalStateException("Transaction " + originalTransaction.getTransactionId() + " of type PAYMENT has incorrect details type.");
                }
                LiabilityPaymentDetails paymentDetails = (LiabilityPaymentDetails) originalDetails;
                
                Optional<Liability> optionalLiabilityToReverse = this.liabilities.stream()
                        .filter(l -> l.getLiabilityId().equals(paymentDetails.getLiabilityId()))
                        .findFirst();
                
                if (optionalLiabilityToReverse.isEmpty()) {
                    throw new IllegalStateException("Liability with ID " + paymentDetails.getLiabilityId() + " not found for voided payment transaction " + originalTransaction.getTransactionId());
                }
                Liability liabilityToReverse = optionalLiabilityToReverse.get();

                liabilityToReverse.reversePayment(paymentDetails.getAmountAppliedToLiability());
                break;

            case CASH_DEPOSIT:
            case CASH_WITHDRAWAL:
                // No specific asset/liability reversal needed beyond cash impact.
                break;

            default:
                // Other transaction types might not have specific entity reversals, or are not yet supported.
                break;
        }

        // --- Record the NEW Reversal Transaction ---
        // This is the immutable record of the voiding event itself.
        Transaction newReversalTransaction = TransactionFactory.createReversalTransaction( // You'll need to add this method to your factory
            UUID.randomUUID(),                          // New transaction ID
            this.portfolioId,
            originalTransaction.getTransactionId(),     // Reference to the original transaction being reversed
            originalType,                               // The type of the original transaction
            originalTransaction.getTotalTransactionAmount(), // The original impact amount for context
            reversalCashFlow,                           // The cash impact of THIS reversal transaction
            Instant.now(),                              // The date of the reversal
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT,"Reversal for " + originalType + " " + originalTransaction.getTransactionId() + ": " + reason, Instant.now(), Instant.now())
            // Fees for the reversal itself are usually zero unless there's a fee for voiding.
            // You might add specific reversal details here if your factory allows more complexity.
        );
        this.transactions.add(newReversalTransaction);

        // --- Update Portfolio's overall timestamp ---
        this.updatedAt = Instant.now();
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
