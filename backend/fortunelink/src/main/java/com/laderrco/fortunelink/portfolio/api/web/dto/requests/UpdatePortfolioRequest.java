package com.laderrco.fortunelink.portfolio.api.web.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for modifying an existing portfolio's metadata")
public record UpdatePortfolioRequest(
    @Schema(description = "Updated name", example = "Global Wealth")
    String name, 
    
    @Schema(description = "Updated description", example = "Updated goals for 2026")
    String description, 
    
    @Schema(description = "Updated base currency code", example = "CAD")
    String currency) {
}