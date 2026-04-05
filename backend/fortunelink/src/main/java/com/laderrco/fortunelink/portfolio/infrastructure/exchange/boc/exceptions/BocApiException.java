package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions;

public class BocApiException extends RuntimeException {
  public BocApiException(String message) {
    super(message);
  }

  public BocApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public BocApiException(String string, int i) {
    super(string + " code: " + i);
  }
}