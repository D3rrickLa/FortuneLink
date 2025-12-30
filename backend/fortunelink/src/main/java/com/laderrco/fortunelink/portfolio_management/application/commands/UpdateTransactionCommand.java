package com.laderrco.fortunelink.portfolio_management.application.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record UpdateTransactionCommand(
    UserId userId,
    AccountId accountId,
    TransactionId transactionId,
    TransactionType type,
    AssetIdentifier identifier,
    BigDecimal quantity,
    Money price,
    List<Fee> fee,
    Instant date,
    String notes
)implements ClassValidation {
    public UpdateTransactionCommand {
        // The validator class will handle this stuff
        // ClassValidation.validateParameter(userId);
        // ClassValidation.validateParameter(transactionId);
        // ClassValidation.validateParameter(accountId);
        // ClassValidation.validateParameter(type);
        // ClassValidation.validateParameter(identifier);
        // ClassValidation.validateParameter(quantity);
        // ClassValidation.validateParameter(price);
        // ClassValidation.validateParameter(fee);
        // ClassValidation.validateParameter(date);
        // ClassValidation.validateParameter(notes);
    }
}