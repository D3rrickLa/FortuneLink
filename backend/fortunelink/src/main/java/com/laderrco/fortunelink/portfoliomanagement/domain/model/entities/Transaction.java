package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.TransactionDate;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.enums.Currency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class Transaction {
    private final TransactionId transactionId;
    private final PortfolioId portfolioId;
    private AccountId accountId;
    private TransactionType transactionType;
    private AssetIdentifier assetIdentifier;
    private Quantity quantity;
    private Price price;

    private List<Fee> fees;
    private TransactionDate transactionDate;
    private String notes;
    private final Instant createdAt;
    

    private Transaction(TransactionId transactionId, PortfolioId portfolioId, AccountId accountId,
            TransactionType transactionType, AssetIdentifier assetIdentifier, Quantity quantity, Price price,
            List<Fee> fees, TransactionDate transactionDate, String notes, Instant createdAt) {
        
        Objects.requireNonNull(transactionId);
        Objects.requireNonNull(portfolioId);
        Objects.requireNonNull(accountId);
        Objects.requireNonNull(transactionType);
        // Objects.requireNonNull(assetIdentifier);
        Objects.requireNonNull(quantity);
        Objects.requireNonNull(price);
        Objects.requireNonNull(fees);
        Objects.requireNonNull(transactionDate);
        Objects.requireNonNull(createdAt);
        

        this.transactionId = transactionId;
        this.portfolioId = portfolioId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.assetIdentifier = assetIdentifier;
        this.quantity = quantity;
        this.price = price;
        this.fees = fees != null ? List.copyOf(fees) : List.of(); // Defensive copy + default
        this.transactionDate = transactionDate;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public static Transaction createBuyTransaction(PortfolioId portfolioId, AccountId accountId, AssetIdentifier assetIdentifier, 
        Quantity quantity, Price price, List<Fee> fees, TransactionDate transactionDate, String notes) {
        validateTransaction(TransactionType.BUY, assetIdentifier, quantity, price);
        return new Transaction(
            TransactionId.randomId(),
            portfolioId,
            accountId,
            TransactionType.BUY,
            assetIdentifier,
            quantity,
            price,
            fees,
            transactionDate,
            notes,
            Instant.now()
        );
    }

    public static Transaction createSellTransaction(PortfolioId portfolioId, AccountId accountId, AssetIdentifier assetIdentifier, 
        Quantity quantity, Price price, List<Fee> fees, TransactionDate transactionDate, String notes) {
        validateTransaction(TransactionType.SELL, assetIdentifier, quantity, price);
        return new Transaction(
            TransactionId.randomId(),
            portfolioId,
            accountId,
            TransactionType.BUY,
            assetIdentifier,
            quantity,
            price,
            fees,
            transactionDate,
            notes,
            Instant.now()
        );
    }

    // create deposit
    public static Transaction createDepositTransaction(PortfolioId portfolioId, AccountId accountId, Price amount, List<Fee> fees, TransactionDate transactionDate, String notes) {
        validateTransaction(TransactionType.DEPOSIT, null, null, amount);
        return new Transaction(
            TransactionId.randomId(),
            portfolioId,
            accountId,
            TransactionType.DEPOSIT,
            null,
            Quantity.ZERO(),
            amount,
            fees,
            transactionDate,
            notes,
            Instant.now()
        );
    }

    // create withdrawal
    public static Transaction createWithdrawalTransaction(PortfolioId portfolioId, AccountId accountId, Price amount, List<Fee> fees, TransactionDate transactionDate, String notes) {
        validateTransaction(TransactionType.WITHDRAWAL, null, null, amount);
        return new Transaction(
            TransactionId.randomId(),
            portfolioId,
            accountId,
            TransactionType.BUY,
            null,
            Quantity.ZERO(),
            amount,
            fees,
            transactionDate,
            notes,
            Instant.now()
        );
    }

    // create dividend 
    public static Transaction createDividendTransaction(PortfolioId portfolioId, AccountId accountId, AssetIdentifier assetIdentifier, Price amount,  TransactionDate transactionDate, String notes) {
        validateTransaction(TransactionType.DIVIDEND, assetIdentifier, Quantity.of("1"), amount);
        return new Transaction(
            TransactionId.randomId(), 
            portfolioId, 
            accountId, 
            TransactionType.DIVIDEND, 
            assetIdentifier, 
            Quantity.of("1"), 
            amount, 
            new ArrayList<>(), 
            transactionDate, 
            notes, 
            Instant.now()
        );

    }

    //create interest
    public static Transaction createInterestTransaction(PortfolioId portfolioId, AccountId accountId, AssetIdentifier assetIdentifier, Price amount,  TransactionDate transactionDate, String notes) {
        validateTransaction(TransactionType.INTEREST, assetIdentifier, Quantity.of("1"), amount);
        return new Transaction(
            TransactionId.randomId(), 
            portfolioId, 
            accountId, 
            TransactionType.INTEREST, 
            assetIdentifier, 
            Quantity.of("1"), 
            amount, 
            new ArrayList<>(), 
            transactionDate, 
            notes, 
            Instant.now()
        );
    }


    public Money calculateTotalCost() {
        return switch (transactionType) {
            case BUY -> calculateGrossAmount().add(calculateTotalFees());
            case SELL -> calculateGrossAmount().subtract(calculateTotalFees()); // Fees reduce proceeds
            case DEPOSIT, WITHDRAWAL -> price.pricePerUnit(); // Already in Money form
            case DIVIDEND, INTEREST -> calculateGrossAmount(); // Usually no fees
            default -> throw new UnsupportedOperationException("Unsupported type: " + transactionType);
        };
    }

    public Money calculateNetAmount() {
        return switch (transactionType) {
            case BUY -> calculateGrossAmount().subtract(calculateTotalFees()); // Net cash outflow
            case SELL -> calculateGrossAmount().add(calculateTotalFees()); // Net cash inflow
            case DEPOSIT -> price.pricePerUnit();
            case WITHDRAWAL -> price.pricePerUnit().negate();
            // etc.
            default -> throw new IllegalArgumentException("Unexpected value: " + transactionType);
        };
    }

    public Money calculateGrossAmount() {
        // price * quantity (before fee)
        return price.pricePerUnit().multiply(quantity.amount());
    }

    public Money calculateTotalFees() {
        // assuming that all the fees have been converted properly
        // public record Fee(FeeType type, Money amount, Money convertedAmount, ExchangeRate exchangeRate, Map<String, String> metadata, Instant conversionDate)
        Currency transactionCurrency = price.getCurrency();
        return this.fees.stream()
            .peek(fee -> {
                if (!fee.convertedAmount().currency().equals(transactionCurrency)) {
                    throw new IllegalStateException("Fee currency mistmatch: expected " + transactionCurrency);
                }
            })
            .map(Fee::convertedAmount)
            .reduce(Money.ZERO(transactionCurrency), Money::add);
    }


    public TransactionId getTransactionId() {
        return transactionId;
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Price getPrice() {
        return price;
    }

    public List<Fee> getFees() {
        return Collections.unmodifiableList(fees);
    }

    public TransactionDate getTransactionDate() {
        return transactionDate;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static void validateTransaction(TransactionType type, AssetIdentifier assetIdentifier, Quantity quantity, Price price) {
        switch (type) {
            case BUY, SELL, DIVIDEND, INTEREST -> {
                if (assetIdentifier == null) {
                    throw new IllegalArgumentException("Asset required for " + type);
                }
                if (quantity.amount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Invalid quantity");
                }
            }
                
            case DEPOSIT, WITHDRAWAL -> {
                // Cash transactions might not need assetIdentifier
                if (price.pricePerUnit().isZero()) throw new IllegalArgumentException("Price required");
        
            }
        
            default -> {
                break;
            }
        }
    }
}
