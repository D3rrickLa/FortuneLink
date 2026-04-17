package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Currency conversion rate information")
public record ExchangeRateResponse(
    @Schema(example = "USD") String from,
    @Schema(example = "CAD") String to,
    @Schema(example = "1.355") BigDecimal rate,
    Instant quotedAt,
    boolean isIdentity) {

  public static ExchangeRateResponse fromDomain(ExchangeRate r) {
    return new ExchangeRateResponse(r.from().getCode(), r.to().getCode(), r.rate(), r.quotedAt(),
        false);
  }

  public static ExchangeRateResponse identity(String currencyCode) {
    return new ExchangeRateResponse(currencyCode, currencyCode, BigDecimal.ONE, Instant.now(),
        true);
  }
}