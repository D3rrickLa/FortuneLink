package com.laderrco.fortunelink.portfolio.domain.model.factories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import java.time.Instant;
import java.util.List;

public final class TransactionFactory {
  private final static Currency CAD = Currency.CAD;

  public static Transaction.TransactionBuilder baseBuilder() {
    return Transaction.builder().transactionId(TransactionId.newId()).accountId(AccountId.newId())
        .cashDelta(Money.zero(CAD)) // Default to zero if not relevant
        .fees(List.of()).metadata(TransactionMetadata.manual(AssetType.STOCK))
        .occurredAt((Instant.now())).notes("");
  }

  public static Transaction.TransactionBuilder sellBuilder(Quantity q, Price p) {
    return baseBuilder().transactionType(TransactionType.SELL)
        .execution(new TradeExecution(new AssetSymbol("AAPL"), q, p))
        .cashDelta(p.calculateValue(q)); // Correctly calculate the delta
  }

  public static Transaction.TransactionBuilder buyBuilder(Quantity q, Price p) {
    Money totalCost = p.calculateValue(q).negate();

    return baseBuilder().transactionType(TransactionType.BUY)
        .execution(new TradeExecution(new AssetSymbol("AAPL"), q, p)).cashDelta(totalCost);
  }
}