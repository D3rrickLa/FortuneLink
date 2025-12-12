package com.laderrco.fortunelink.portfolio_management.application.commands;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record RecordDepositCommand(UserId userId, AccountId accountId, Money amount, ValidatedCurrency currency, List<Money> fees, Instant transactionDate, String notes) {
}