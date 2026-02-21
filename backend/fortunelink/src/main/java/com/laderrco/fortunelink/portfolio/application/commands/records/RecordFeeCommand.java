package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;

// this might need working as the 'Fee' class has more info in it
// This makes no sense, how can a 'fee' have a 'fee' list
// or money amount can be total amount
public record RecordFeeCommand(PortfolioId portfolioId, AccountId accountId, Money totalAmount, Currency currency, List<Fee> fees, Instant transactionDate, String notes) {
}