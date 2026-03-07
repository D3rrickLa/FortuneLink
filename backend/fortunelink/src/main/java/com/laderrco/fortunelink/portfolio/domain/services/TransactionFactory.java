package com.laderrco.fortunelink.portfolio.domain.services;

import java.time.Instant;
import java.util.List;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionDate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;

public class TransactionFactory {
  public static Transaction buy(Account account, AssetSymbol symbol, AssetType type,
      Quantity quantity, Price price, List<Fee> fees, String notes, Instant date) {

    List<Fee> feeList = fees != null ? fees : List.of();
    Money totalFee = Fee.totalInAccountCurrency(feeList, account.getAccountCurrency());
    Money grossCost = price.pricePerUnit().multiply(quantity);

    Money totalOutflow = grossCost.add(totalFee);
    new Transaction(TransactionId.newId(), account.getAccountId(), TransactionType.BUY,
        new TradeExecution(symbol, quantity, price), null, totalOutflow.negate(), fees, notes,
        TransactionDate.of(date), null, TransactionMetadata.manual(type));
  }
}
