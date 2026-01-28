package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DeletePortfolioRequest {

    @NotBlank(message = "Portfolio ID is required")
    private String portfolioId;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Confirmed flag is required")
    private Boolean confirmed;

    @NotNull(message = "Soft delete flag is required")
    private Boolean softDelete;

    public DeletePortfolioRequest(String portfolioId, String userId, Boolean confirmed, Boolean softDelete) {
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.confirmed = confirmed;
        this.softDelete = softDelete;
    }

    // Getters
    public String getPortfolioId() { return portfolioId; }
    public String getUserId() { return userId; }
    public Boolean getConfirmed() { return confirmed; }
    public Boolean getSoftDelete() { return softDelete; }
}