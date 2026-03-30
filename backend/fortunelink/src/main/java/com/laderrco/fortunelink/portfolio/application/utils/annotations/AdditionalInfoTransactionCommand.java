package com.laderrco.fortunelink.portfolio.application.utils.annotations;

import java.time.Instant;

public interface AdditionalInfoTransactionCommand extends TransactionCommand {
  Instant transactionDate();

  String notes();
}
