package com.laderrco.fortunelink.portfolio_management.application.commands.records;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;



// String symbol is fine, we are just recording the name, not the entity
// asset symbol - name
// asset entity - your identifier calss, the acutal holding
public record RecordPurchaseCommand(PortfolioId portfolioId, UserId userId, AccountId accountId, String symbol, Quantity quantity, Price price, List<Fee> fees, Instant transactionDate, String notes) {

}
