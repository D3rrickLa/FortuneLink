package com.laderrco.fortunelink.portfolio_management.application.queries;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

/* 
    Instant startDate, Instant endDate,
        TransactionType transactionType, AccountId accountId

        are optional
*/
public record GetTransactionHistoryQuery(UserId userId, Instant startDate, Instant endDate,
        TransactionType transactionType, AccountId accountId, int pageNumber, int pageSize) {

}
