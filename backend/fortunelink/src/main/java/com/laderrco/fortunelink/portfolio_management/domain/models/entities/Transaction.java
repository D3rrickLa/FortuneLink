package com.laderrco.fortunelink.portfolio_management.domain.models.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Getter;

@Getter // TODO: Remove this later
public class Transaction implements ClassValidation {
    // TODO: look at and compare if V2 Transaction makes sense of V5_1
    private final TransactionId transacationId;
    private TransactionType transactionType;
    private AssetIdentifier assetIdentifier;
    private BigDecimal quantity;
    private Money pricePerUnit;
    private List<Fee> fees; // fees in original currency with a exchange rate link to the portfolios
    private Instant transactionDate;
    private String notes;

    public Transaction(TransactionId transacationId, TransactionType transactionType, AssetIdentifier assetIdentifier,
            BigDecimal quantity, Money pricePerUnit, List<Fee> fees, Instant transactionDate, String notes) {

        
        this.transacationId = ClassValidation.validateParameter(transacationId);
        this.transactionType = ClassValidation.validateParameter(transactionType);
        this.assetIdentifier = ClassValidation.validateParameter(assetIdentifier);
        this.quantity = ClassValidation.validateParameter(quantity);
        this.pricePerUnit = ClassValidation.validateParameter(pricePerUnit);
        this.fees = fees != null ? fees : Collections.emptyList();
        this.transactionDate = ClassValidation.validateParameter(transactionDate);
        this.notes = notes.isBlank() ? "" : notes;
    }

    public Money calculateTotalCost() {
        return null; // before fees
    }

    public Money calculateNetAmount() {
        return null; // after fees
    }


    
}
