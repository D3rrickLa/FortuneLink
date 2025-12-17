package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// for the services
// Single transaction representation; contains all transaction details
// TODO: check if the 'symbol' is what we really want and not assetidentifier UPDATE: this this is fine, keeping as it
public record TransactionResponse(TransactionId transactionId, TransactionType type, String symbol, BigDecimal quantity, Money price, List<Fee> fees, Money totalCost, Instant date, String notes) {
}