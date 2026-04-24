package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Detailed breakdown of a single fee associated with a transaction")
public record FeeRequest(
    @Schema(description = "Category of the fee")
    @NotNull FeeType feeType, 
    
    @Schema(description = "Cost of the fee", example = "9.99")
    @NotNull BigDecimal amount, 
    
    @Schema(description = "Currency the fee was charged in", example = "USD")
    @NotBlank String currency) {
}