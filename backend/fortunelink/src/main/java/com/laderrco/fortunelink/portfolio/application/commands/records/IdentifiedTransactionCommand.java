package com.laderrco.fortunelink.portfolio.application.commands.records;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;

public interface IdentifiedTransactionCommand extends TransactionCommand {
  TransactionId transactionId();
}