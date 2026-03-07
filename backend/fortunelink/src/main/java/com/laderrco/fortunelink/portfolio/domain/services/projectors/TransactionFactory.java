package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
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
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;

@Component
public class TransactionFactory {
  public Transaction buy(Account account, AssetSymbol symbol, AssetType type, Quantity quantity,
      Price price, List<Fee> fees, String notes, Instant date) {

    List<Fee> feeList = fees != null ? fees : List.of();

    Money totalFee = Fee.totalInAccountCurrency(feeList, account.getAccountCurrency());

    Money grossCost = price.pricePerUnit().multiply(quantity);

    Money totalOutflow = grossCost.add(totalFee);

    return new Transaction(TransactionId.newId(), account.getAccountId(), TransactionType.BUY,
        new TradeExecution(symbol, quantity, price), null, totalOutflow.negate(), feeList, notes,
        TransactionDate.of(date), null, TransactionMetadata.manual(type));
  }
}
