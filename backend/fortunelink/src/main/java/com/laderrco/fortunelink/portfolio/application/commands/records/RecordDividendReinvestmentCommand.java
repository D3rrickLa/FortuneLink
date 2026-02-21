package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record RecordDividendReinvestmentCommand(
        PortfolioId portfolioId,
        UserId userId,
        AccountId accountId,
        String assetSymbol,
        DripExecution execution, // encapsulates the trade details
        Instant transactionDate,
        String notes) implements TransactionCommand {

    public record DripExecution(
            Quantity sharesPurchased,
            Price pricePerShare // explicit, not derived
    ) {
        public Money totalCost() {
            return pricePerShare.pricePerUnit().multiply(sharesPurchased.amount());
        }
    }
}
