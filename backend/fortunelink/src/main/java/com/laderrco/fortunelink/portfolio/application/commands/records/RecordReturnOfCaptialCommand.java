package com.laderrco.fortunelink.portfolio.application.commands.records;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;

public record RecordReturnOfCaptialCommand(
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    String assetSymbol,
    Price distributionPerUnit,
    Quantity heldQuantity,
    String notes,
    Instant transactionDate) implements TransactionCommand {

}
