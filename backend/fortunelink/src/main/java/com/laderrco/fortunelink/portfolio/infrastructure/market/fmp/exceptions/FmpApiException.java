package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.exceptions;

public class FmpApiException extends RuntimeException {
  public FmpApiException(String message) {
    super(message);
  }

  public FmpApiException(String message, Throwable cause) {
    super(message, cause);
  }
}