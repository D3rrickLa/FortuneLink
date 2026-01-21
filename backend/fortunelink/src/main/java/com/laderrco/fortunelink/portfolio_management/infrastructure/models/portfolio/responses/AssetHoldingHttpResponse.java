package com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for asset holding details.
 * 
 * Example JSON:
 * {
 *   "id": "asset-123",
 *   "symbol": "AAPL",
 *   "assetType": "STOCK",
 *   "quantity": 10,
 *   "costBasis": 1500.00,
 *   "acquiredDate": "2024-01-15T10:00:00"
 * }
 */
public record AssetHoldingHttpResponse(
    String id,
    String symbol,
    String assetType,
    BigDecimal quantity,
    BigDecimal costBasis,
    LocalDateTime acquiredDate
) {}