package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FeeRequest(
    @NotNull FeeType feeType, @NotNull BigDecimal amount, @NotBlank String currency) {
}