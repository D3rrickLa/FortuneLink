package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record RecordIncomeCommand(
        PortfolioId portfolioId,
        UserId userId,
        AccountId accountId,
        String assetSymbol,
        Money amount,
        TransactionType type,
        boolean isDrip,
        Quantity sharesReceived,
        Instant transactionDate,
        String notes
) implements TransactionCommand {
    public RecordIncomeCommand {
        if (isDrip && (sharesReceived == null || sharesReceived.compareTo(Quantity.ZERO) <= 0)) {
            throw new IllegalArgumentException(
                    "Shares received must be provided and greater than zero for DRIP transaction"
            );
        }
    }

    // NOTE: we might want to do some inner record like 'execution' in Transaction
}
