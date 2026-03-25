package com.laderrco.fortunelink.portfolio.application.exceptions;

public class AccountCannotBeReopenedException extends RuntimeException {
  public AccountCannotBeReopenedException(String message) {
    super(message);
  }
}
