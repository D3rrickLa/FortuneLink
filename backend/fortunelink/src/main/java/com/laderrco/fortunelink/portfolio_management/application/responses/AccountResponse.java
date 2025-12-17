package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record AccountResponse(AccountId accountId, String name, AccountType type, ValidatedCurrency baseCurrency, Money cashBalance, Money totalValue, Instant createdDate) implements ClassValidation {
    public AccountResponse { 
        ClassValidation.validateParameter(accountId);
        ClassValidation.validateParameter(name);
        ClassValidation.validateParameter(type);
        ClassValidation.validateParameter(baseCurrency);
        ClassValidation.validateParameter(cashBalance);
        ClassValidation.validateParameter(totalValue);
        ClassValidation.validateParameter(createdDate);
    }
}