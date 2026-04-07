package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import jakarta.validation.constraints.NotNull;

public record ExcludeTransactionRequest(@NotNull String reason) {

}
