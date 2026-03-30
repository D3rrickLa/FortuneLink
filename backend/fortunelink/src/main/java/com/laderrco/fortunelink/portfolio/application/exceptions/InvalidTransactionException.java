package com.laderrco.fortunelink.portfolio.application.exceptions;

public class InvalidTransactionException extends RuntimeException {
  public InvalidTransactionException(String s) {
    super(s); // need message and validationerrors to be passed
  }
}
