package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new portfolio.
 * 
 * Example JSON:
 * {
 *   "userId": "user-123",
 *   "name": "My Retirement Portfolio",
 *   "description": "Long-term retirement savings"
 * }
 */
public record CreatePortfolioRequest(
    
    @NotBlank(message = "User ID is required")
    UUID userId,
    
    @NotBlank(message = "Portfolio name is required")
    String name,
    
    @NotBlank(message = "Currency preference cannot be blank")
    String currencyPreference,

    String description, // Optional

    boolean createAccount
) {
    // Compact constructor for validation
    public CreatePortfolioRequest {
        if (name != null && name.length() > 100) {
            throw new IllegalArgumentException("Portfolio name cannot exceed 100 characters");
        }
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("Description cannot exceed 500 characters");
        }
    }
}