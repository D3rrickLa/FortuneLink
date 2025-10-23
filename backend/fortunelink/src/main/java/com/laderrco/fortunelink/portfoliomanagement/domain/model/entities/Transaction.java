package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.util.List;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Price;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.TransactionDate;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class Transaction {
    private final TransactionId transactionId;
    private final PortfolioId portfolioId;
    private AccountId accountId;
    private TransactionType transactionType;
    private Quantity quantity;
    private Price price;
    private List<Fee> fees;
    private TransactionDate transactionDate;
    private String notes;
    
    public Transaction(TransactionId transactionId, PortfolioId portfolioId, AccountId accountId,
            TransactionType transactionType, Quantity quantity, Price price, List<Fee> fees,
            TransactionDate transactionDate, String notes) {
        this.transactionId = transactionId;
        this.portfolioId = portfolioId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.price = price;
        this.fees = fees;
        this.transactionDate = transactionDate;
        this.notes = notes;
    }

    public static Transaction createBuyTransaction() {
        return null;
    }

    public Money calculateTotalCost() {
        // (price * quantity) + fee
        return null;
    }

    public Money calculateNetAmount() {
        // (price * quantity) - fee
        return null;
    }

    public Money calculateGrossAmount() {
        // price * quantity (before fee)
        return null;
    }
}
