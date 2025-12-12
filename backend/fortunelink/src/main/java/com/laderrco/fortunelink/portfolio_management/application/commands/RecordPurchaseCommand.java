package com.laderrco.fortunelink.portfolio_management.application.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// String symbol is fine, we are just recording the name, not the entity
// asset symbol - name
// asset entity - your identifier calss, the acutal holding
public record RecordPurchaseCommand(UserId userId, AccountId accountId, String symbol, BigDecimal quantity, Money price, List<Fee> fees, Instant transactionDate, String notes) {

}
