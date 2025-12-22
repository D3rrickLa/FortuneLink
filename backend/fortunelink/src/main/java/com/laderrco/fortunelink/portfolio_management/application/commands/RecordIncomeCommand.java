package com.laderrco.fortunelink.portfolio_management.application.commands;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// might want to make a IncomeType.java
public record RecordIncomeCommand(UserId userId, AccountId accountId, String symbol, Money amount, TransactionType type, boolean isDrip, BigDecimal sharesRecieved, Instant transactionDate, String notes) {
    public RecordIncomeCommand {
        /*
            true null
            true !null, negative
            true !null, 0
            true !null, 1

            false null
            false !null, negative
            false !null, 0
            false !null, 1
        */
        if (isDrip && (sharesRecieved == null || sharesRecieved.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Shares recieved must be provided for DRIP transaction");
        }
    }
}