package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.exceptions.InvalidQuantityException;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Builder
@Getter // TODO: Remove this later
// TODO: we shold look back to version 2 with the diffrence transaction detail objects as we need to cram a lot of stuff in Transaction.java
public class Transaction implements ClassValidation {
    // TODO: look at and compare if V2 Transaction makes sense of V5_1
    private final TransactionId transactionId;
    private TransactionType transactionType;
    private AssetIdentifier assetIdentifier; 
    private BigDecimal quantity; 
    private Money pricePerUnit; // this need to act as both the ppu and an amount for deposit and withdrawal 
    private List<Fee> fees; // fees in original currency with a exchange rate link to the portfolios
    private Instant transactionDate;
    private String notes;

    public Transaction(TransactionId transactionId, TransactionType transactionType, AssetIdentifier assetIdentifier, BigDecimal quantity, Money pricePerUnit, List<Fee> fees, Instant transactionDate, String notes) {
        validateTransaction(transactionType, assetIdentifier, quantity, pricePerUnit);

        this.transactionId = ClassValidation.validateParameter(transactionId);
        this.transactionType = ClassValidation.validateParameter(transactionType);
        this.assetIdentifier = ClassValidation.validateParameter(assetIdentifier);
        this.quantity = ClassValidation.validateParameter(quantity);
        this.pricePerUnit = ClassValidation.validateParameter(pricePerUnit);
        this.fees = fees != null ? fees : Collections.emptyList();
        this.transactionDate = ClassValidation.validateParameter(transactionDate);
        this.notes = notes.isBlank() ? "" : notes.trim();
    }

    /**
     * This returns the 'total amount exchanged of the transaction' 
     * so for a 'buy' order you are buying the stock + any other fees
     * while for a 'sell' you are getting your money minus any other fees
     * @return
     */
    public Money calculateTotalCost() {
        return switch (transactionType) {
            case BUY -> calculateGrossAmount().add(calculateTotalFees());
            case SELL -> calculateGrossAmount().subtract(calculateTotalFees()); // Fees reduce proceeds
            case DEPOSIT, WITHDRAWAL -> pricePerUnit; // Already in Money form
            case DIVIDEND, INTEREST -> calculateGrossAmount(); // Usually no fees
            default -> throw new UnsupportedOperationException("Unsupported type: " + transactionType);
        };
    }

    public Money calculateNetAmount() {
        return switch (transactionType) {
            case BUY -> calculateGrossAmount().subtract(calculateTotalFees()); // Net cash outflow
            case SELL -> calculateGrossAmount().add(calculateTotalFees()); // Net cash inflow
            case DEPOSIT -> pricePerUnit;
            case WITHDRAWAL -> pricePerUnit.negate();
            // etc.
            default -> throw new IllegalArgumentException("Unexpected value: " + transactionType);
        };
    }

   public Money calculateGrossAmount() {
        // price * quantity (before fee)
        return pricePerUnit.multiply(quantity);
    }

    public Money calculateTotalFees() {
        // assuming that all the fees have been converted properly
        ValidatedCurrency transactionCurrency = pricePerUnit.currency();
        return this.fees.stream()
            .peek(fee -> {
                if (!fee.amountInNativeCurrency().currency().equals(transactionCurrency)) {
                    throw new CurrencyMismatchException("Fee currency mistmatch: expected " + transactionCurrency);
                }
            })
            .map(Fee::amountInNativeCurrency)
            .reduce(Money.ZERO(transactionCurrency), Money::add);
    }

    private static void validateTransaction(TransactionType type, AssetIdentifier assetIdentifier, BigDecimal quantity, Money price) {
        switch (type) {
            case BUY, SELL, DIVIDEND, INTEREST -> {
                if (assetIdentifier == null) {
                    throw new IllegalArgumentException("Asset required for " + type);
                }
                if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new InvalidQuantityException("Invalid quantity");
                }
                if (price.amount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Invalid price");
                }
            }
                
            case DEPOSIT, WITHDRAWAL -> {
                // Cash transactions might not need assetIdentifier
                if (price.isZero()) throw new IllegalArgumentException("Price required");
        
            }
        
            default -> {
                break;
            }
        }
    }
}
