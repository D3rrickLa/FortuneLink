package com.laderrco.fortunelink.portfolio.infrastructure.exceptions;

public class UnknownSymbolException extends RuntimeException {
  public UnknownSymbolException(String message) {
    super(message);
  }
}
