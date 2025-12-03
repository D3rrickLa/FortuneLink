package com.laderrco.fortunelink.portfolio_management.application.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record UpdateTransactionCommand(UserId userId, TransactionId transactionId, String symbol, BigDecimal quantity, Money price, List<Fee> fees, Instant transactionDate, String notes) {
    
}
