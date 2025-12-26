package com.laderrco.fortunelink.portfolio_management.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

/* 
    Instant startDate, Instant endDate, TransactionType transactionType, AccountId accountId are optional
*/
public record GetTransactionHistoryQuery(UserId userId, AccountId accountId, Instant startDate, Instant endDate, TransactionType transactionType, int pageNumber, int pageSize) implements ClassValidation {
    public GetTransactionHistoryQuery {
        ClassValidation.validateParameter(userId);
        ClassValidation.validateParameter(accountId);
        ClassValidation.validateParameter(startDate);
        ClassValidation.validateParameter(endDate);
        ClassValidation.validateParameter(transactionType);
        ClassValidation.validateParameter(pageNumber);
        ClassValidation.validateParameter(pageSize);
    }
}
