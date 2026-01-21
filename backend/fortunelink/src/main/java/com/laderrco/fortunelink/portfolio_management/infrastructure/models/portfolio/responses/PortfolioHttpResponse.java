package com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.responses;

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
    LocalDateTime createdDate,
    LocalDateTime lastUpdated
) {}