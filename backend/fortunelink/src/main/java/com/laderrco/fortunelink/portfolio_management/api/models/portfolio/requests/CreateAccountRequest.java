package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
public class CreateAccountRequest {

    @NotBlank(message = "Account name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Account type is required")
    private String accountType;  // From the AccounType

    @NotBlank(message = "Base currency is required")
    private String baseCurrency;  // CAD, USD, EUR, etc.

    // All-args constructor
    public CreateAccountRequest(String name, String accountType, String baseCurrency) {
        this.name = name == null ? "DEFAULT_NAME" : name;
        this.accountType = accountType == null ? "NON_REGISTERED" : accountType;
        this.baseCurrency = baseCurrency == null ? "USD" : baseCurrency;
    }

    // Getters
    public String getName() { return name; }
    public String getAccountType() { return accountType; }
    public String getBaseCurrency() { return baseCurrency; }
}
