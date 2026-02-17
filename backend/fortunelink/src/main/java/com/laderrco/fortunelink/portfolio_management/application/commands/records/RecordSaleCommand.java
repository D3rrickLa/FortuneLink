package com.laderrco.fortunelink.portfolio_management.application.commands.records;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public record RecordSaleCommand(PortfolioId portfolioId, UserId userId, AccountId accountId, AssetId assetId, BigDecimal quantity, Money price, List<Fee> fees, Instant transactionDate, String notes) {
    
}
