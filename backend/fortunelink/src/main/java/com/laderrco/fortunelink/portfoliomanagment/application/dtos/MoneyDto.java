package com.laderrco.fortunelink.portfoliomanagment.application.dtos;

import java.math.BigDecimal;
import java.util.Objects;

public record MoneyDto(
    BigDecimal amount,
    String currencyCode
) {
    public MoneyDto {
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(currencyCode, "Currency code cannot be null.");
    }
}
