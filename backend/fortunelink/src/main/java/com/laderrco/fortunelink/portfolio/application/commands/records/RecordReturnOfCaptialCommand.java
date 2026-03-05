package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public record RecordReturnOfCaptialCommand(PortfolioId portfolioId, UserId userId,
                AccountId accountId, String assetSymbol, Price distributionPerUnit,
                Quantity heldQuantity, Instant transactionDate, String notes)
                implements TransactionCommand {

}
