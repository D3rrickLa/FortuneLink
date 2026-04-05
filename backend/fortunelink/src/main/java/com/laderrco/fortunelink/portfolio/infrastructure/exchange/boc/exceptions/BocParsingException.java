package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions;

import tools.jackson.core.JacksonException;

public class BocParsingException extends RuntimeException {
  public BocParsingException(String string, JacksonException e) {
    super(string, e);
  }
}