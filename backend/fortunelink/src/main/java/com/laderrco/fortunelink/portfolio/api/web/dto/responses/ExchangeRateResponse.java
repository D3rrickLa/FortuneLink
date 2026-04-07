package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import java.math.BigDecimal;
import java.time.Instant;

public record ExchangeRateResponse(
    String from, String to, BigDecimal rate, Instant quotedAt, boolean isIdentity) {

  public static ExchangeRateResponse fromDomain(ExchangeRate r) {
    return new ExchangeRateResponse(r.from().getCode(), r.to().getCode(), r.rate(), r.quotedAt(),
        false);
  }

  public static ExchangeRateResponse identity(String currencyCode) {
    return new ExchangeRateResponse(currencyCode, currencyCode, BigDecimal.ONE, Instant.now(),
        true);
  }
}