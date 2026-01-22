package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses;

import java.util.List;

/**
 * Response DTO for account details.
 * 
 * Example JSON:
 * {
 *   "id": "account-123",
 *   "name": "My TFSA",
 *   "accountType": "TFSA",
 *   "baseCurrency": "CAD",
 *   "assets": [...]
 * }
 */
public record AccountHttpResponse(
    String id,
    String portfolioId,
    String name,
    String accountType,
    String baseCurrency,
    List<AssetHoldingHttpResponse> assets
) {}