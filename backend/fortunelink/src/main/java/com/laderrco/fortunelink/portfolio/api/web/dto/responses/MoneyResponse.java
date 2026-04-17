package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * API-safe serialization of the Money domain value object.
 * <p>
 * The domain Money type serializes as: {"amount": {"amount": 100.00,
 * "currency": {"currencyCode":
 * "CAD", ...}}}
 * <p>
 * which is deeply nested, exposes internal domain structure, and is fragile to
 * internal
 * refactoring. This DTO flattens it to: {"amount": 100.00, "currency": "CAD"}
 * <p>
 * Use this in every response record that needs to express a monetary value.
 * Never return raw Money
 * domain objects through the API boundary.
 */
@Schema(description = "Universal money representation for high-precision values")
public record MoneyResponse(
    @Schema(example = "1250.75") BigDecimal amount,
    @Schema(example = "USD") String currency) {
  public static MoneyResponse from(Money money) {
    if (money == null) {
      return null;
    }
    return new MoneyResponse(money.amount(), money.currency().getCode());
  }

  public static MoneyResponse zero(String currencyCode) {
    return new MoneyResponse(BigDecimal.ZERO, currencyCode);
  }
}
