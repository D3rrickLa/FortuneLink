package com.laderrco.fortunelink.portfolio_management.application.commands;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record RecordIncomeCommand(
        PortfolioId portfolioId,
        UserId userId,
        AccountId accountId,
        AssetId assetId,
        Money amount,
        TransactionType type,
        boolean isDrip,
        BigDecimal sharesReceived,
        Instant transactionDate,
        String notes
) {
    public RecordIncomeCommand {
        if (isDrip && (sharesReceived == null || sharesReceived.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException(
                    "Shares received must be provided and greater than zero for DRIP transaction"
            );
        }
    }
}
