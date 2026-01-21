package com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new account within a portfolio.
 * 
 * Example JSON:
 * {
 *   "name": "My TFSA",
 *   "accountType": "TFSA",
 *   "baseCurrency": "CAD"
 * }
 */
public record CreateAccountRequest(
    
    @NotBlank(message = "Account name is required")
    String name,
    
    @NotBlank(message = "Account type is required")
    String accountType,  // TFSA, RRSP, NON_REGISTERED, etc.
    
    @NotBlank(message = "Base currency is required")
    String baseCurrency  // CAD, USD, EUR, etc.
) {
    public CreateAccountRequest {
        if (name != null && name.length() > 100) {
            throw new IllegalArgumentException("Account name cannot exceed 100 characters");
        }
    }
}