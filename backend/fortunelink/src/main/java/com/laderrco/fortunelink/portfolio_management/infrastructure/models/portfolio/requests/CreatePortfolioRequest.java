package com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.requests;

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
    String userId,
    
    @NotBlank(message = "Portfolio name is required")
    String name,
    
    String description  // Optional
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