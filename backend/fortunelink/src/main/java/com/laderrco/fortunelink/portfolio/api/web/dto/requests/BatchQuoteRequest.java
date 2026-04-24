package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for retrieving multiple asset quotes in one call")
public record BatchQuoteRequest(
    @Schema(description = "List of ticker symbols (e.g., AAPL, BTC-USD)", example = "[\"AAPL\", \"MSFT\"]") @NotNull @Size(min = 1, max = 20, message = "Batch quote requests are limited to 20 symbols") List<String> symbols) {
}