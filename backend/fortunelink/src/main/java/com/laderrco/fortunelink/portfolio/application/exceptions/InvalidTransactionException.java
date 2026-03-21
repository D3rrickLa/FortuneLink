package com.laderrco.fortunelink.portfolio.application.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import java.util.List;

public class InvalidTransactionException extends RuntimeException {
  public InvalidTransactionException(String s) {
    super(s); // need message and validationerrors to be passed
  }

  public InvalidTransactionException(String s, List<String> errors) {
    super(String.format("Error with message: %s. List of validation errors", s, errors.stream()));
  }

  public InvalidTransactionException(TransactionId transactionId) {
    this("Transaction not found with id: " + transactionId.id().toString());
  }
}
