package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new portfolio.
 * 
 * Example JSON:
 * {
 * "userId": "user-123",
 * "name": "My Retirement Portfolio",
 * "description": "Long-term retirement savings"
 * }
 */
public class CreatePortfolioRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Portfolio name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Currency preference cannot be blank")
    private String currencyPreference;

    @Size(max = 500)
    private String description;

    private Boolean createAccount;

    // All-args constructor
    // NOTE: We had to use a ternary operation because for some odd reason, @Valid on the controller side
    // jsut isn't 'seeing' that we are assigning values 
    public CreatePortfolioRequest(String userId, String name, String currencyPreference, String description, Boolean createAccount) {
        this.userId = userId == null ? UUID.randomUUID().toString() : userId;
        this.name = name == null ? "ERROR" : name;
        this.currencyPreference = currencyPreference == null ? "USD" : currencyPreference;
        this.description = description;
        this.createAccount = createAccount;
    }

    // Getters (needed for Jackson)
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getCurrencyPreference() { return currencyPreference; }
    public String getDescription() { return description; }
    public Boolean getCreateAccount() { return createAccount; }
}