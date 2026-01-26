package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;

public record FeeRequest(
        FeeType type,
        BigDecimal amount,
        String currency,
        LocalDateTime feeDate,
        Map<String, String> metadata
) {}