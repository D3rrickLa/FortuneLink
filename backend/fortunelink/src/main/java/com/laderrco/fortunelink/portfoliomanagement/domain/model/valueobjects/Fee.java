package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.FeeType;
import com.laderrco.fortunelink.shared.exception.CurrencyMismatchException;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record Fee(FeeType type, Money amount, Money convertedAmount, ExchangeRate exchangeRate, Map<String, String> metadata, Instant conversionDate) {
    public Fee {
        validateParameter(type, "type");
        validateParameter(amount, "amount");
        validateParameter(convertedAmount, "convertedAmount");
        validateParameter(metadata, "exchangeRate");
        validateParameter(conversionDate, "conversionDate");
    }
    
    
    public Fee(FeeType feeType, Money amount) {
        this(feeType, amount, amount, ExchangeRate.create(amount.currency(), amount.currency(), 0.0, Instant.now()), new HashMap<>(), Instant.now());
    }


    public Money apply(Money baseAmount) {
        Objects.requireNonNull(baseAmount);
        if (!amount.currency().equals(baseAmount.currency())) {
            if (!exchangeRate.to().equals(baseAmount.currency())) {
                throw new CurrencyMismatchException("Fees cannot be subtracted base currency. Fees are not the same currency and or unknown exchange rate");
            } 
            else {
                return baseAmount.subtract(exchangeRate.convert(amount));
            }
        }
        else {
            return baseAmount.subtract(amount);
        }
    }

    private void validateParameter(Object other, String parameterName) {
        Objects.requireNonNull(other, String.format("%s cannot be null", parameterName));
    }

}
