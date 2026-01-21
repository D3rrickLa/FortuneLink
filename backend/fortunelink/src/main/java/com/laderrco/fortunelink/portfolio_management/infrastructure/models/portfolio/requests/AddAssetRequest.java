package com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding an asset to an account.
 * 
 * Example JSON:
 * {
 *   "symbol": "AAPL",
 *   "assetType": "STOCK",
 *   "quantity": 10,
 *   "costBasis": 1500.00,
 *   "acquiredDate": "2024-01-15T10:00:00"
 * }
 */
public record AddAssetRequest(
    
    @NotBlank(message = "Asset symbol is required")
    String symbol,
    
    @NotBlank(message = "Asset type is required")
    String assetType,  // STOCK, ETF, CRYPTO, etc.
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    BigDecimal quantity,
    
    @NotNull(message = "Cost basis is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost basis must be positive")
    BigDecimal costBasis,
    
    @NotNull(message = "Acquired date is required")
    LocalDateTime acquiredDate
) {}