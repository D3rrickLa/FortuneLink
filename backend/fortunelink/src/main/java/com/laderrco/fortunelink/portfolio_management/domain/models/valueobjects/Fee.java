package com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Builder;

// TODO: Remove Builder later on and implement your own
@Builder
public record Fee(FeeType feeType, Money amountInNativeCurrency, ExchangeRate exchangeRate, Map<String, String> metadata, Instant feeDate) implements ClassValidation {
    public Fee { 
        feeType = ClassValidation.validateParameter(feeType, "Fee Type");
        amountInNativeCurrency = ClassValidation.validateParameter(amountInNativeCurrency, "Amount In Native Currency");
        exchangeRate = ClassValidation.validateParameter(exchangeRate, "Exchange Rate");
        feeDate = ClassValidation.validateParameter(feeDate, "Fee Date");

        if (amountInNativeCurrency.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fee amount cannot be negative."); // TODO: InvalidQuantityException.class
        }

        metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }
    
}
