package com.laderrco.fortunelink.portfolio_management.application.commands;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// this might need working as the 'Fee' class has more infor in it
public record RecordFeeCommand(UserId userId, AccountId accountId, Money amount, ValidatedCurrency currency, List<Fee> fees, Instant transactionDate, String notes) {
}