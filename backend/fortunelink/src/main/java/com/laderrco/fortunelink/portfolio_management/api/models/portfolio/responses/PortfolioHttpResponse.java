package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for portfolio details.
 * 
 * Example JSON:
 * {
 *   "id": "portfolio-123",
 *   "userId": "user-456",
 *   "name": "My Retirement Portfolio",
 *   "description": "Long-term savings",
 *   "accounts": [...],
 *   "createdDate": "2024-01-01T00:00:00",
 *   "lastUpdated": "2024-01-15T10:30:00"
 * }
 */
public record PortfolioHttpResponse(
    String id,
    String userId,
    String name,
    String description,
    List<AccountHttpResponse> accounts,
    BigDecimal totalValue,
    String totalValueCurrency,
    LocalDateTime createdDate,
    LocalDateTime lastUpdated
) {
    public PortfolioHttpResponse {}

    public static PortfolioHttpResponse of(
        String id,
        String userId,
        String name,
        String description,
        List<AccountHttpResponse> accounts,
        LocalDateTime createdDate,
        LocalDateTime lastUpdated
    ) {
        return new PortfolioHttpResponse(id, userId, name, description, accounts, null, description, createdDate, lastUpdated);
    }
}