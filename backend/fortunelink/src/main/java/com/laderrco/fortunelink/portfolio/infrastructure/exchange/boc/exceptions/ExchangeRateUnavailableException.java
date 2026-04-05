package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions;

import java.time.Instant;

public class ExchangeRateUnavailableException extends RuntimeException {

  public ExchangeRateUnavailableException(String code, String code2, Instant asOf) {
    super(String.format("cannot find %s to %s on %s", code, code2, asOf));
  }

}
