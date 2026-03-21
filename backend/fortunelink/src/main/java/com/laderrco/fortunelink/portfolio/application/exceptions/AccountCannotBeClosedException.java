package com.laderrco.fortunelink.portfolio.application.exceptions;

public class AccountCannotBeClosedException extends RuntimeException {

  public AccountCannotBeClosedException(String message) {
    super(message);
  }
}
