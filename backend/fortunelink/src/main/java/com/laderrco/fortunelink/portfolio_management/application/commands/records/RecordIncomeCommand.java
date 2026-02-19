package com.laderrco.fortunelink.portfolio_management.application.commands.records;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;

public record RecordIncomeCommand(
        PortfolioId portfolioId,
        UserId userId,
        AccountId accountId,
        AssetSymbol assetSymbol,
        Money amount,
        TransactionType type,
        boolean isDrip,
        Quantity sharesReceived,
        Instant transactionDate,
        String notes
) {
    public RecordIncomeCommand {
        if (isDrip && (sharesReceived == null || sharesReceived.compareTo(Quantity.ZERO) <= 0)) {
            throw new IllegalArgumentException(
                    "Shares received must be provided and greater than zero for DRIP transaction"
            );
        }
    }
}
