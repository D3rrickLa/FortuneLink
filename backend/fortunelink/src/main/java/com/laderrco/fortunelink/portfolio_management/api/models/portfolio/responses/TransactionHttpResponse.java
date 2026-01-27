package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for transaction details.
 * 
 * Example JSON:
 * {
 *   "id": "txn-123",
 *   "accountId": "account-456",
 *   "transactionType": "BUY",
 *   "symbol": "AAPL",
 *   "quantity": 10,
 *   "price": 150.25,
 *   "fee": 9.99,
 *   "totalCost": 1512.49,
 *   "transactionDate": "2024-01-15T10:30:00",
 *   "notes": "Initial purchase",
 *   "recordedAt": "2024-01-15T10:30:05"
 * }
 */
public record TransactionHttpResponse(
    String id,
    String accountId,
    String transactionType,
    String symbol,
    BigDecimal quantity,
    BigDecimal price,
    String priceCurrency,
    BigDecimal fee,
    BigDecimal totalCost,    // price * quantity + fee
    BigDecimal netAmount,    // For sells: price * quantity - fee
    LocalDateTime transactionDate,
    String notes,
    LocalDateTime recordedAt  // When this transaction was recorded in the system
) {}