package com.laderrco.fortunelink.portfoliomanagement.domain.factories;

// this class is meant for to be flexxible in terms of storing a specific transaction type (i.e. liability vs. asset transaction)
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Transaction;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetTransferDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CorporateActionTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.VoidTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

// Precision used in Money class and Quantities variables
// totalTransactionAmount should always reflect the net cash change: Positive for inflow, Negative for outflow.
/*
 * NOTE: so when we look at actual transaction in a brokage account, all values are positive expect for 'fees' or 'cash back'
 * this system, internal, deals with signed values which is more accurate in terms of money management. We would use a display layer
 * to actual show positive/abs values
 * TODO: FINAL all the variables in the headers, mainly the UUIDs ALSO we need to remove all logic in the methods, the builder is for making Transaction, no logic
 */
public class TransactionFactory {
        // include everything for transaction + things related to the
        // entityTransactionDetails

        // problem, we need to know what we have bought, right now we only know if it's
        // a stock/etf or crypto through AssetIdentifer
        // the IF statement right now seems to fix that issue, but say if the asset we
        // bought were bonds, like acutal T-Bills
        // and not ETFs of bonds, that will pose a problem + any other asset in the
        // future we could think of.
        // for now though I think we are sticking to use STOCKS - on the exchange and
        // crypto
        public static Transaction createBuyAssetTransaction(UUID transactionId, UUID portfolioId, AssetIdentifier assetIdentifier, Instant transactionDate, BigDecimal quantity, Money pricePerUnit, TransactionMetadata transactionMetadata, List<Fee> associatedFees) {

                Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
                Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
                Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
                Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
                Objects.requireNonNull(quantity, "Quantity cannot be null.");
                Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null.");
                Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");


                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive for a buy transaction.");
                }
                if (pricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Price per unit must be positive for a buy transaction.");
                }
                if (transactionMetadata.transactionStatus() != TransactionStatus.ACTIVE && transactionMetadata.transactionStatus() != TransactionStatus.PENDING) {
                        throw new IllegalArgumentException("Transaction Status you want to void MUST be active or pending.");
                }

                RoundingMode roundingMode = RoundingMode.HALF_UP;
                int quantityDecimalPlaces = assetIdentifier.assetType().getDefaultQuantityPrecision().getDecimalPlaces();
                int cashDecimalPlaces = DecimalPrecision.CASH.getDecimalPlaces();

                BigDecimal normalizedQuantity = quantity.setScale(quantityDecimalPlaces, roundingMode);
                Money normalizedPricePerUnit = pricePerUnit.setScale(cashDecimalPlaces, roundingMode);
                Money costOfAssets = normalizedPricePerUnit.multiply(normalizedQuantity).setScale(cashDecimalPlaces,
                                roundingMode);

                Money totalFees = associatedFees != null
                                ? associatedFees.stream().map(Fee::amount)
                                                .reduce(Money.ZERO(pricePerUnit.currency()), Money::add)
                                                .setScale(cashDecimalPlaces, roundingMode)
                                : Money.ZERO(pricePerUnit.currency());
                Money totalTransactionAmount = costOfAssets.add(totalFees).negate().setScale(cashDecimalPlaces,
                                roundingMode); // outflow

                AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, normalizedQuantity, normalizedPricePerUnit);

                return new Transaction.Builder()
                                .transactionId(transactionId)
                                .portfolioId(portfolioId)
                                .transactionType(TransactionType.BUY)
                                .totalTransactionAmount(totalTransactionAmount)
                                .transactionDate(transactionDate)
                                .transactionDetails(assetTransactionDetails)
                                .transactionMetadata(transactionMetadata)
                                .fees(associatedFees)
                                .build();
        }

        public static Transaction createSellAssetTransaction(UUID transactionId, UUID portfolioId, AssetIdentifier assetIdentifier, Instant transactionDate, BigDecimal quantity, Money pricePerUnit, TransactionMetadata transactionMetadata, List<Fee> associatedFees) {

                Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
                Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
                Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
                Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
                Objects.requireNonNull(quantity, "Quantity cannot be null.");
                Objects.requireNonNull(pricePerUnit, "Price per unit cannot be null.");
                Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");

                // Semantic validation for numerical inputs
                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive for a sell transaction.");
                }
                if (pricePerUnit.amount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Price per unit must be positive for a sell transaction.");
                }
                if (transactionMetadata.transactionStatus() != TransactionStatus.ACTIVE && transactionMetadata.transactionStatus() != TransactionStatus.PENDING) {
                        throw new IllegalArgumentException("Transaction Status you want to void MUST be active or pending.");
                }

                RoundingMode roundingMode = RoundingMode.HALF_UP;
                int quantityDecimalPlaces = assetIdentifier.assetType().getDefaultQuantityPrecision()
                                .getDecimalPlaces();
                int cashDecimalPlaces = DecimalPrecision.CASH.getDecimalPlaces();

                BigDecimal normalizedQuantity = quantity.setScale(quantityDecimalPlaces, roundingMode);
                Money normalizedPricePerUnit = pricePerUnit.setScale(cashDecimalPlaces, roundingMode);
                Money costOfAssetSale = normalizedPricePerUnit.multiply(normalizedQuantity).setScale(cashDecimalPlaces,
                                roundingMode);

                Money totalFees = associatedFees != null
                                ? associatedFees.stream().map(Fee::amount)
                                                .reduce(Money.ZERO(pricePerUnit.currency()), Money::add)
                                                .setScale(cashDecimalPlaces, roundingMode)
                                : Money.ZERO(pricePerUnit.currency());
                Money totalTransactionAmount = costOfAssetSale.subtract(totalFees).setScale(cashDecimalPlaces,
                                roundingMode); // inflow (after fees)

                AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, normalizedQuantity, normalizedPricePerUnit);

                return new Transaction.Builder()
                                .transactionId(transactionId)
                                .portfolioId(portfolioId)
                                .transactionType(TransactionType.SELL)
                                .totalTransactionAmount(totalTransactionAmount)
                                .transactionDate(transactionDate)
                                .transactionDetails(assetTransactionDetails)
                                .transactionMetadata(transactionMetadata)
                                .fees(associatedFees)
                                .build();
        }

        // amount is always positive, we negate at the end if withdrawal
        public static Transaction createCashTransaction(UUID transactionId, UUID portfolioId, TransactionType transactionType, Instant transactionDate, 
                        Money originalCashflowAmount, Money currencyPrefConvertedCashflowAmount, BigDecimal exchangeRate, Money totalFOREXConversionFeesInPortfolioCurrency, 
                        Money totalOtherFeesInPortfolioCurrency, Money netPortfolioCashImpact, // this is the final net chang so from 100 USD -> 129.30 (after fees)
                        TransactionMetadata transactionMetadata, List<Fee> associatedFees) {
                Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
                Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
                Objects.requireNonNull(transactionType, "Transaction type cannot be null.");
                Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
                Objects.requireNonNull(originalCashflowAmount, "Amount cannot be null.");

                Objects.requireNonNull(currencyPrefConvertedCashflowAmount, "Converted cashflow amount cannot be null.");
                Objects.requireNonNull(exchangeRate, "Exchange rate cannot be null.");
                Objects.requireNonNull(totalFOREXConversionFeesInPortfolioCurrency, "Total Forex conversion fee cannot be null.");
                Objects.requireNonNull(totalOtherFeesInPortfolioCurrency, "Total other fees  cannot be null.");
                Objects.requireNonNull(netPortfolioCashImpact, "Net portfolio cash impact cannot be null.");
                
                Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");
                Objects.requireNonNull(associatedFees, "Fee list cannot be null.");
                
                // we need to negate at the portfolio level
                // if (originalCashflowAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
                //         throw new IllegalArgumentException("Amount must be positive for cash transaction.");
                // }
                if (transactionMetadata.transactionStatus() != TransactionStatus.COMPLETED) {
                        throw new IllegalArgumentException("Transaction status must be COMPLETED for a new cash transaction.");
                }

                if (exchangeRate.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Exchange rate provided cannot be negative.");
                }
                if (totalFOREXConversionFeesInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Foreign exchange rate fees provided cannot be negative.");
                }
                if (totalOtherFeesInPortfolioCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Total other fees provided cannot be negative.");
                }

                CashflowTransactionDetails cashTransactionDetails = new CashflowTransactionDetails(originalCashflowAmount, currencyPrefConvertedCashflowAmount, exchangeRate, totalFOREXConversionFeesInPortfolioCurrency, totalOtherFeesInPortfolioCurrency);
                return new Transaction.Builder()
                        .transactionId(transactionId)
                        .portfolioId(portfolioId)
                        .transactionType(transactionType)
                        .totalTransactionAmount(netPortfolioCashImpact)
                        .transactionDate(transactionDate)
                        .transactionDetails(cashTransactionDetails)
                        .transactionMetadata(transactionMetadata)
                        .fees(associatedFees)
                        .build();

        }

        // the worry of the negate function is unfound. because the original buy and sell of items are porsitive (inflow) 
        // and negative (outflow), the negate would reverse all transaction thus we don't need to worry about what 
        // type of VOID_ it is
        // check if status is active? <- need to add that for all methods in the factory
        public static Transaction createVoidTransaction(UUID transactionId, UUID portfolioId, UUID originalTransactionId, TransactionType transactionType, Money originalTransactionAmount, Instant transactionDate, String reason, TransactionMetadata transactionMetadata) {
                Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
                Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
                Objects.requireNonNull(originalTransactionId, "Original Transaction ID cannot be null.");
                Objects.requireNonNull(transactionType, "Void transaction type cannot be null.");
                Objects.requireNonNull(originalTransactionAmount, "Original Amount cannot be null.");
                Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
                Objects.requireNonNull(reason, "Reason for voiding cannot be null.");
                Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");
                
                if (!transactionType.name().startsWith("VOID_")) {
                        throw new IllegalArgumentException("TransactionType must be a VOID_ type for this method.");
                }
                // TODO: other checks for createVoidTransaction
                // NOTE: we  keep the voidInfo because of querying and performance as well as audit, and filtering

                if (transactionMetadata.transactionStatus() != TransactionStatus.ACTIVE && transactionMetadata.transactionStatus() != TransactionStatus.PENDING) {
                        throw new IllegalArgumentException("Transaction Status you want to void MUST be active or pending.");
                }

                Money totalTransactionAmount = originalTransactionAmount.negate().setScale(DecimalPrecision.CASH.getDecimalPlaces(), RoundingMode.HALF_UP);
                VoidTransactionDetails voidTransactionDetails = new VoidTransactionDetails(originalTransactionId, reason);
                
                // Optional<VoidInfo> voidInfo = Optional.ofNullable(new VoidInfo(originalTransactionId));
                // NOTE:                         .voidInfo(voidInfo) // status of the original transacion and who voided it
                //  that code is wrong, if placed in the new transaction, we are automatically saying that the transaction was voided
                // what we need to do is get the transaction with the originalTransactionId and void it with the transactionId
                // this needs to be done at a higher level as we are just making constructors 
                // this method is just to create new VOID_ tpye transaction

                return new Transaction.Builder()
                        .transactionId(transactionId)
                        .portfolioId(portfolioId)
                        .transactionType(transactionType)
                        .totalTransactionAmount(totalTransactionAmount)
                        .transactionDate(transactionDate)
                        .transactionDetails(voidTransactionDetails) // *what* a void transaction is doing and why
                        .voidInfo(Optional.empty()) // who did the voiding
                        .transactionMetadata(transactionMetadata)
                        .build();
        }

        // was missing assetidentifer, quantity, costbasis, and fees
        // source can be null/special ID for external
        // costbasis is nullable, only needed if tracking cost basis for transfers
        public static Transaction createAssetTransferInTransaction(UUID transactionId, UUID destinationPortfolioId, UUID sourcePortfolioId, AssetIdentifier assetIdentifier, PortfolioCurrency portfolioCurrencyPref, Instant transactionDate, BigDecimal quantity, Money costBasisPerUnit, TransactionMetadata transactionMetadata, List<Fee> fees) {
                // asset moving in and out of other accoutns (not a sell but shifting ownersh

                // OBJECTS NULLS AND EXCEPTION CHECKS HERE
                   Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
                   Objects.requireNonNull(destinationPortfolioId, "Destination Portfolio ID cannot be null.");
                   Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
                   Objects.requireNonNull(portfolioCurrencyPref, "Portfolio currency preference cannot be null.");
                   Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
                   Objects.requireNonNull(quantity, "Quantity cannot be null.");
                   if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                       throw new IllegalArgumentException("Quantity must be positive for transfer in.");
                   }
                // fees can be null or empty list. If fees are null, make it an empty list.
                // costBasisPerUnit might be null if it's not tracked for external transfers.

                if (transactionMetadata.transactionStatus() != TransactionStatus.ACTIVE && transactionMetadata.transactionStatus() != TransactionStatus.PENDING) {
                        throw new IllegalArgumentException("Transaction Status you want to void MUST be active or pending.");
                }


                RoundingMode roundingMode = RoundingMode.HALF_UP;
                int quantityDecimalPlaces = assetIdentifier.assetType().getDefaultQuantityPrecision().getDecimalPlaces();
                int cashDecimalPlaces = DecimalPrecision.CASH.getDecimalPlaces();

                BigDecimal normalizedQuantity = quantity.setScale(quantityDecimalPlaces, roundingMode);
                Money normalizedCostBasis = (costBasisPerUnit != null) ? costBasisPerUnit.setScale(cashDecimalPlaces, roundingMode) : null;
                
                Money totalFees = fees != null
                                ? fees.stream().map(Fee::amount)
                                                .reduce(Money.ZERO(portfolioCurrencyPref), Money::add)
                                                .setScale(cashDecimalPlaces, roundingMode)
                                : Money.ZERO(portfolioCurrencyPref);

                Money totalTransactionAmount = totalFees.negate().setScale(cashDecimalPlaces, roundingMode);
                AssetTransferDetails assetTransferDetails = new AssetTransferDetails(destinationPortfolioId, sourcePortfolioId, assetIdentifier, normalizedQuantity, normalizedCostBasis);

                return new Transaction.Builder()
                        .transactionId(transactionId)
                        .portfolioId(destinationPortfolioId)
                        .transactionType(TransactionType.TRANSFER_IN)
                        .totalTransactionAmount(totalTransactionAmount) //fees only
                        .transactionDate(transactionDate)
                        .transactionDetails(assetTransferDetails)
                        .transactionMetadata(transactionMetadata)
                        .fees(fees)
                        .build();
        }
        
        // will do this after the transfer in
        public static Transaction createAssetTransferOutTransaction(UUID transactionId, UUID destinationPortfolioId, UUID sourcePortfolioId, AssetIdentifier assetIdentifier, PortfolioCurrency portfolioCurrencyPref, Instant transactionDate, BigDecimal quantity, Money costBasisPerUnit, TransactionMetadata transactionMetadata, List<Fee> fees) {
                // asset moving in and out of other accoutns (not a sell but shifting ownersh {
                // OBJECTS NULLS AND EXCEPTION CHECKS HERE
                Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
                Objects.requireNonNull(sourcePortfolioId, "Source Portfolio ID cannot be null for TRANSFER_OUT."); // destination can be null for transfer to external accounts
                Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
                Objects.requireNonNull(portfolioCurrencyPref, "Portfolio currency preference cannot be null.");
                Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
                Objects.requireNonNull(quantity, "Quantity cannot be null.");
                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                       throw new IllegalArgumentException("Quantity must be positive for transfer out.");
                }
                // fees can be null or empty list. If fees are null, make it an empty list.
                // costBasisPerUnit might be null if it's not tracked for external transfers.
                if (transactionMetadata.transactionStatus() != TransactionStatus.ACTIVE && transactionMetadata.transactionStatus() != TransactionStatus.PENDING) {
                        throw new IllegalArgumentException("Transaction Status you want to void MUST be active or pending.");
                }

                RoundingMode roundingMode = RoundingMode.HALF_UP;
                int quantityDecimalPlaces = assetIdentifier.assetType().getDefaultQuantityPrecision().getDecimalPlaces();
                int cashDecimalPlaces = DecimalPrecision.CASH.getDecimalPlaces();

                BigDecimal normalizedQuantity = quantity.setScale(quantityDecimalPlaces, roundingMode);
                Money normalizedCostBasis = (costBasisPerUnit != null) ? costBasisPerUnit.setScale(cashDecimalPlaces, roundingMode) : null;
                
                Money totalFees = fees != null
                                ? fees.stream().map(Fee::amount)
                                                .reduce(Money.ZERO(portfolioCurrencyPref), Money::add)
                                                .setScale(cashDecimalPlaces, roundingMode)
                                : Money.ZERO(portfolioCurrencyPref); // costBasistPerUnit can be null, so we rely on the portfolio currency

                Money totalTransactionAmount = totalFees.negate().setScale(cashDecimalPlaces, roundingMode);
                AssetTransferDetails assetTransferDetails = new AssetTransferDetails(destinationPortfolioId, sourcePortfolioId, assetIdentifier, normalizedQuantity, normalizedCostBasis);

                return new Transaction.Builder()
                        .transactionId(transactionId)
                        .portfolioId(sourcePortfolioId)
                        .transactionType(TransactionType.TRANSFER_OUT)
                        .totalTransactionAmount(totalTransactionAmount) //fees only
                        .transactionDate(transactionDate)
                        .transactionDetails(assetTransferDetails)
                        .transactionMetadata(transactionMetadata)
                        .fees(fees)
                        .build();
        }
        
        // missing the asset identifier
        // NOTE: splitRatio is nullable as it's only relevant for splits, this also could be in the details object...
        // we would want to handle things like mergers, spin-offs, bond calls, etc.
        public static Transaction createCorporateActionTransaction(UUID transactionId, UUID portfolioId, TransactionType transactionType, PortfolioCurrency portfolioCurrencyPref, AssetIdentifier assetIdentifier, BigDecimal splitRatio, Instant transactionDate, TransactionMetadata transactionMetadata) {
                // for non-cash events that alter your asset holdings (i.e. stock splits)
                // totalTransactionAmount would be zero typically, it's about your position
                Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
                Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
                Objects.requireNonNull(transactionType, "Transaction type cannot be null.");
                Objects.requireNonNull(portfolioCurrencyPref, "Currency preference cannot be null.");
                Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
                Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
                Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");

                // Validate splitRatio based on transaction type
                if (transactionType == TransactionType.STOCK_SPLIT || transactionType == TransactionType.REVERSE_STOCK_SPLIT) {
                        Objects.requireNonNull(splitRatio, "Split ratio is required for STOCK_SPLIT or REVERSE_STOCK_SPLIT transaction types.");
                        if (splitRatio.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Split ratio must be positive for stock splits.");
                        }
                } else if (transactionType == TransactionType.CORPORATE_ACTION) {
                        // For generic CORPORATE_ACTION, splitRatio is not applicable.
                        // Ensure it's null, or if not, throw an error to prevent misuse.
                        if (splitRatio != null) {
                        throw new IllegalArgumentException("Split ratio must be null for generic CORPORATE_ACTION transaction type, as it's not applicable.");
                        }
                        // If you reached here, splitRatio is either already null (as expected) or was set to null by the previous check.
                } else {
                        // Handle any other unexpected TransactionType values here if necessary
                        throw new IllegalArgumentException("Unsupported TransactionType for Corporate Action: " + transactionType);
                }

                if (transactionMetadata.transactionStatus() != TransactionStatus.ACTIVE && transactionMetadata.transactionStatus() != TransactionStatus.PENDING) {
                        throw new IllegalArgumentException("Transaction Status you want to void MUST be active or pending.");
                }

                CorporateActionTransactionDetails corporateActionTransactionDetails = new CorporateActionTransactionDetails(assetIdentifier, splitRatio);

                return new Transaction.Builder()
                .transactionId(transactionId)
                .portfolioId(portfolioId)
                .transactionType(transactionType) // Type derived from the details object
                .totalTransactionAmount(Money.ZERO(portfolioCurrencyPref)) // Typically zero cash impact
                .transactionDate(transactionDate)
                .transactionDetails(corporateActionTransactionDetails) // Pass the specific details object
                .transactionMetadata(transactionMetadata)
                .fees(Collections.emptyList()) // Assuming no fees for corporate actions        
                .build();
        }

}