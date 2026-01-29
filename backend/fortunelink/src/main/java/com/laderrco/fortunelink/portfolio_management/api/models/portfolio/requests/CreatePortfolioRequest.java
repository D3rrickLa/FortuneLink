package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

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
@ToString
@EqualsAndHashCode
public class CreatePortfolioRequest {
    @Size(max = 100)
    @JsonProperty("name")
    @NotBlank(message = "Portfolio name is required")
    private String name;

    @JsonProperty("currencyPreference")
    @NotBlank(message = "Currency preference cannot be blank")
    private String currencyPreference;

    @Size(max = 500)
    @JsonProperty("description")
    private String description;

    @JsonProperty("createAccount")
    private Boolean createAccount;

    // All-args constructor
    // NOTE: We had to use a ternary operation because for some odd reason, @Valid
    // on the controller side
    // jsut isn't 'seeing' that we are assigning values
    public CreatePortfolioRequest(String name, String currencyPreference, String description, Boolean createAccount) {
        this.name = name;
        this.currencyPreference = currencyPreference;
        this.description = description;
        this.createAccount = createAccount;
    }

    // Getters (needed for Jackson)
    public String getName() {
        return name;
    }

    public String getCurrencyPreference() {
        return currencyPreference;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getCreateAccount() {
        return createAccount;
    }
}