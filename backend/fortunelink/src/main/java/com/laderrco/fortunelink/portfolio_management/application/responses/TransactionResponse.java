package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// for the services
// Single transaction representation; contains all transaction details
// TODO: check if the 'symbol' is what we really want and not assetidentifier UPDATE: this this is fine, keeping as it
public record TransactionResponse(TransactionId transactionId, TransactionType type, String symbol, BigDecimal quantity, Money price, List<Fee> fees, Money totalCost, Instant date, String notes) implements ClassValidation {
    public TransactionResponse {
        // we might not even need this if you think about it because the validation result should handle everything, maybe just the response, but the commands are handled
        ClassValidation.validateParameter(transactionId);
        ClassValidation.validateParameter(type);
        ClassValidation.validateParameter(symbol);
        ClassValidation.validateParameter(quantity);
        ClassValidation.validateParameter(price);
        ClassValidation.validateParameter(fees);
        ClassValidation.validateParameter(totalCost);
        ClassValidation.validateParameter(date);
        // ClassValidation.validateParameter(notes);
    }

    public Money setNetAmount() {
        Optional<Money> totalFees = fees.stream()
            .map(f -> f.amountInNativeCurrency())
            .reduce(Money::add);
        
        if (totalFees.isPresent()) {
            return totalCost.subtract(totalFees.get());
        }
        return totalCost;
    }
}