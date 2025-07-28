package com.laderrco.fortunelink.portfoliomanagment.application.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;

public record PortfolioDetailsDto(
    UUID portfolioId,
    String name,
    String description,
    Money cashBalance,            // Direct from Portfolio
    Money totalMarketValue,       // Calculated by Portfolio
    Money unrealizedGains,        // Calculated by Portfolio
    Money totalLiabilitiesValue,  // Calculated by Portfolio
    Money netWorth,               // Calculated by Portfolio
    List<AssetHoldingDto> assetHoldings,   // List of nested DTOs
    List<LiabilityDto> liabilities,        // List of nested DTOs
    List<TransactionDto> recentTransactions // List of nested DTOs
) {
    public PortfolioDetailsDto {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(name, "Name cannot be null.");
        Objects.requireNonNull(cashBalance, "Cash balance cannot be null.");
        Objects.requireNonNull(totalMarketValue, "Total market value cannot be null.");
        Objects.requireNonNull(unrealizedGains, "Unrealized gains cannot be null.");
        Objects.requireNonNull(totalLiabilitiesValue, "Total liabilities value cannot be null.");
        Objects.requireNonNull(netWorth, "Net worth cannot be null.");
        Objects.requireNonNull(assetHoldings, "Asset holdings list cannot be null.");
        Objects.requireNonNull(liabilities, "Liabilities list cannot be null.");
        Objects.requireNonNull(recentTransactions, "Recent transactions list cannot be null.");
    }

    // This is the specific DTO you asked about. It represents a single AssetHolding for display.
    public record AssetHoldingDto(
        UUID assetHoldingId,
        String assetSymbol,           // e.g., "AAPL"
        String assetName,             // e.g., "Apple Inc."
        String assetType,             // e.g., "STOCK", "ETF"
        BigDecimal quantity,          // Current quantity held
        Money averageCostBasis,       // Average cost for this holding
        Money currentMarketPrice,     // Current price per unit obtained from PricingService
        Money currentHoldingValue,    // Calculated: quantity * currentMarketPrice
        Money unrealizedGainLoss      // Calculated: currentHoldingValue - totalCostBasis
    ) {
        public AssetHoldingDto {
            Objects.requireNonNull(assetHoldingId, "Asset Holding ID cannot be null.");
            Objects.requireNonNull(assetSymbol, "Asset symbol cannot be null.");
            Objects.requireNonNull(assetName, "Asset name cannot be null.");
            Objects.requireNonNull(assetType, "Asset type cannot be null.");
            Objects.requireNonNull(quantity, "Quantity cannot be null.");
            Objects.requireNonNull(averageCostBasis, "Average cost basis cannot be null.");
            Objects.requireNonNull(currentMarketPrice, "Current market price cannot be null.");
            Objects.requireNonNull(currentHoldingValue, "Current holding value cannot be null.");
            Objects.requireNonNull(unrealizedGainLoss, "Unrealized gain/loss cannot be null.");
        }
    }

    // DTO for a single Liability for display
    public record LiabilityDto(
        UUID liabilityId,
        String description,
        Money originalAmount,
        Money currentBalance,
        Percentage annualInterestRate,
        Instant incurrenceDate,
        Instant maturityDate,
        boolean isPaidOff
    ) {
        public LiabilityDto {
            Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");
            Objects.requireNonNull(description, "Description cannot be null.");
            Objects.requireNonNull(originalAmount, "Original amount cannot be null.");
            Objects.requireNonNull(currentBalance, "Current balance cannot be null.");
            Objects.requireNonNull(annualInterestRate, "Annual interest rate cannot be null.");
            Objects.requireNonNull(incurrenceDate, "Incurrence date cannot be null.");
            Objects.requireNonNull(maturityDate, "Maturity date cannot be null.");
        }
    }

    // DTO for a single Transaction for display
    public record TransactionDto(
        UUID transactionId,
        TransactionType type,
        Instant transactionDate,
        String description,
        Money amount,
        String assetSymbol // Can be null if it's a cash transaction
    ) {
        public TransactionDto {
            Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
            Objects.requireNonNull(type, "Transaction type cannot be null.");
            Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
            Objects.requireNonNull(description, "Description cannot be null.");
            Objects.requireNonNull(amount, "Amount cannot be null.");
        }
    }
}
