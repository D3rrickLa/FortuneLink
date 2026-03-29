package com.laderrco.fortunelink.portfolio.application.utils.annotations;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;

public interface IdentifiedTransactionCommand extends TransactionCommand {
  TransactionId transactionId();
}