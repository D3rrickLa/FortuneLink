// this class is meant for to be flexxible in terms of storing a specific transaction type (i.e. liability vs. asset transaction)

package com.laderrco.fortunelink.PortfolioManagement.domain.factories;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Transaction;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.CashTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Fee;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionMetadata;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.VoidTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.DecimalPrecision;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

// Precision used in Money class and Quantities variables
// totalTransactionAmount should always reflect the net cash change: Positive for inflow, Negative for outflow.
/*
 * NOTE: so when we look at actual transaction in a brokage account, all values are positive expect for 'fees' or 'cash back'
 * this system, internal, deals with signed values which is more accurate in terms of money management. We would use a display layer
 * to actual show positive/abs values
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

                RoundingMode roundingMode = RoundingMode.HALF_UP;
                int quantityDecimalPlaces = assetIdentifier.assetType().getDefaultQuantityPrecision()
                                .getDecimalPlaces();
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

        // amount is always positive
        public static Transaction createCashTransaction(UUID transactionId, UUID portfolioId, TransactionType transactionType, Instant transactionDate, Money amount, TransactionMetadata transactionMetadata, List<Fee> associatedFees) {
                Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
                Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
                Objects.requireNonNull(transactionType, "Transaction type cannot be null.");
                Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
                Objects.requireNonNull(amount, "Amount cannot be null.");
                Objects.requireNonNull(transactionMetadata, "Transaction metadata cannot be null.");
                
                if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Amoutn must be positive for cash transaction.");
                }

                RoundingMode roundingMode = RoundingMode.HALF_UP;
                int cashDecimalPlaces = DecimalPrecision.CASH.getDecimalPlaces();
                Money normalizedAmount = amount.setScale(cashDecimalPlaces, roundingMode);

                Money totalTransactionAmount;
                switch (transactionType) {
                        case DEPOSIT:
                        case INTEREST:
                        case DIVIDEND:
                                totalTransactionAmount = normalizedAmount;
                                break;
                        case WITHDRAWAL:
                        case EXPENSE:
                        case FEE:
                                totalTransactionAmount = normalizedAmount.negate();
                                break;
                        case BUY:
                        case SELL:
                        case PAYMENT:
                        case VOID_BUY:
                        case VOID_SELL:
                        case VOID_WITHDRAWAL:
                                throw new IllegalArgumentException("Invalid TransactionType for cash for cash transaction: " + transactionType);
                
                        default:
                                throw new IllegalArgumentException("Unsupported TransactionType: " + transactionType);
                }

                Money totalFees = associatedFees != null
                        ? associatedFees.stream().map(Fee::amount)
                                        .reduce(Money.ZERO(amount.currency()), Money::add)
                                        .setScale(cashDecimalPlaces, roundingMode)
                        : Money.ZERO(amount.currency());
                
                // Adjust totalTransactionAmount for associatedFees if applicable
                totalTransactionAmount = totalTransactionAmount.add(totalFees.negate()).setScale(cashDecimalPlaces, roundingMode);

                CashTransactionDetails cashTransactionDetails = new CashTransactionDetails(normalizedAmount);

                return new Transaction.Builder()
                        .transactionId(transactionId)
                        .portfolioId(portfolioId)
                        .transactionType(transactionType)
                        .totalTransactionAmount(totalTransactionAmount)
                        .transactionDate(transactionDate)
                        .transactionDetails(cashTransactionDetails)
                        .transactionMetadata(transactionMetadata)
                        .fees(associatedFees)
                        .build();

        }

        // the worry of the negate function is unfound. because the original buy and sell of items are porsitive (inflow) 
        // and negative (outflow), the negate would reverse all transaction thus we don't need to worry about what 
        // type of VOID_ it is
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

        public Transaction createAssetTransferInTransaction(UUID transactionId, UUID destinationPortfolioId, UUID sourcePortfolioId, Instant interactionDate, TransactionMetadata transactionMetadata) {
                // asset moving in and out of other accoutns (not a sell but shifting ownership)
                throw new UnsupportedOperationException("Need implementation");
        }
        
        // will do this after the transfer in
        public Transaction createAssetTransferOutTransaction() {
                throw new UnsupportedOperationException("Need implementation");
        }
        
        public Transaction createCorporateActionTransaction(UUID transactionId, UUID portfolioId, TransactionType transactionType, BigDecimal splitRatio, Instant interactionDate, TransactionMetadata transactionMetadata) {
                // for non-cash events that alter your asset holdings (i.e. stock splits)
                // totalTransactionAmoutn would be zero typically, it's about your position
                throw new UnsupportedOperationException("Need implementation");
        }

}