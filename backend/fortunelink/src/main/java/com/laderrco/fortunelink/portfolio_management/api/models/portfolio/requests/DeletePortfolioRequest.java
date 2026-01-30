package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import jakarta.validation.constraints.NotNull;

public class DeletePortfolioRequest {
    @NotNull(message = "Confirmed flag is required")
    private Boolean confirmed;

    @NotNull(message = "Soft delete flag is required")
    private Boolean softDelete;

    public DeletePortfolioRequest( Boolean confirmed, Boolean softDelete) {
        this.confirmed = confirmed;
        this.softDelete = softDelete;
    }

    // Getters
    public Boolean getConfirmed() { return confirmed; }
    public Boolean getSoftDelete() { return softDelete; }
}