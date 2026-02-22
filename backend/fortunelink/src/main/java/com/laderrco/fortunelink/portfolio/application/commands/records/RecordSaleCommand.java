package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;


public record RecordSaleCommand(PortfolioId portfolioId, UserId userId, AccountId accountId, String symbol, Quantity quantity, Price price, List<Fee> fees, Instant transactionDate, String notes) implements TransactionCommand {
    public Money totalFees(Currency currency) {
        return Fee.totalInAccountCurrency(fees, currency);
    }
}
