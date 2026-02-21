package com.laderrco.fortunelink.portfolio_management.application.commands.records;

import java.time.Instant;
import java.util.List;

// this might need working as the 'Fee' class has more info in it
// This makes no sense, how can a 'fee' have a 'fee' list
// or money amount can be total amount
public record RecordFeeCommand(PortfolioId portfolioId, AccountId accountId, Money totalAmount, ValidatedCurrency currency, List<Fee> fees, Instant transactionDate, String notes) {
}