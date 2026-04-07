package com.laderrco.fortunelink.portfolio.application.commands.records;

import com.laderrco.fortunelink.portfolio.application.utils.annotations.AdditionalInfoTransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.UUID;

// asset symbol can be nullable (when null, it means cash/account-level interest)
public record RecordInterestCommand(
    UUID idempotencyKey,
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    String assetSymbol,
    Money amount,
    Instant transactionDate,
    String notes) implements AdditionalInfoTransactionCommand {
  // Cash/account-level interest (HISA, savings)
  public static RecordInterestCommand cashInterest(UUID idempotencyKey, PortfolioId portfolioId,
      UserId userId, AccountId accountId, Money amount, Instant date, String notes) {
    return new RecordInterestCommand(idempotencyKey, portfolioId, userId, accountId, null, amount,
        date, notes);
  }

  // Asset-level interest (bond coupons)
  public static RecordInterestCommand assetInterest(UUID idempotencyKey, PortfolioId portfolioId,
      UserId userId, AccountId accountId, String symbol, Money amount, Instant date, String notes) {
    return new RecordInterestCommand(idempotencyKey, portfolioId, userId, accountId, symbol, amount,
        date, notes);
  }

  public boolean isAssetInterest() {
    return assetSymbol != null && !assetSymbol.isBlank();
  }
}
