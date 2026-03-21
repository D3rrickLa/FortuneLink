package com.laderrco.fortunelink.portfolio.application.mappers;

import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TransactionViewMapper {

  public TransactionView toTransactionView(Transaction transaction) {
    if (transaction.execution() == null) {
      return new TransactionView(transaction.transactionId(), transaction.transactionType(), null,
          // no need for symbols for 'i.e. deposit'
          null, null, // no price for these types of transaction
          transaction.fees(), transaction.cashDelta(), transaction.metadata().asFlatMap(),
          transaction.occurredAt(), transaction.notes());
    }
    return new TransactionView(transaction.transactionId(), transaction.transactionType(),
        transaction.execution().asset().symbol(), transaction.execution().quantity(),
        transaction.execution().pricePerUnit(), transaction.fees(), transaction.cashDelta(),
        transaction.metadata().asFlatMap(), transaction.occurredAt(), transaction.notes());
  }

  public List<TransactionView> toViewList(List<Transaction> transactions) {
    if (transactions == null || transactions.isEmpty()) {
      return List.of();
    }

    return transactions.stream().map(this::toTransactionView).toList();
  }
}