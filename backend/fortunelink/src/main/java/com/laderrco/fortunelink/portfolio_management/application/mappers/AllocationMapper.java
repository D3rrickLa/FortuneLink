package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationDetail;
import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

// converts allocation calculations to response format - format percentgaes and categoreis
public class AllocationMapper {

    private AllocationMapper() {
        // to prevent instantiation 
    }

    public static AllocationResponse toResponse(Map<String, Money> allocation, Money totalValue, Instant asOfDate) {
        Instant responseDate = asOfDate != null ? asOfDate : Instant.now();

        if (allocation == null || allocation.isEmpty()) {
            return new AllocationResponse(
                new HashMap<>(),
                // Use a helper or ensure consistent zero handling
                // for default, will just use USD
                totalValue != null ? totalValue : Money.ZERO("USD"), 
                responseDate
            );
        }

        Map<String, AllocationDetail> allocationDetails = allocation.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> toAllocationDetail(entry.getKey(), entry.getValue(), totalValue)
            ));
        
        return new AllocationResponse(
            allocationDetails, 
            totalValue != null ? totalValue : Money.ZERO("USD"), 
            responseDate
        );
    }
    
    public static AllocationDetail toAllocationDetail(String category, Money value, Money total) {
        if (category == null || value == null) {
            throw new IllegalArgumentException("Category and value cannot be null");
        }
        
        AssetType assetType;
        try {
            assetType = AssetType.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid asset type category: " + category, e);
        }
        
        return new AllocationDetail(assetType, value, calculatePercentage(value, total));
    }
    
    /**
     * Converts a map of AssetType allocations to an AllocationResponse.
     * This is a type-safe alternative when working with AssetType enums directly.
     * 
     * @param allocation Map of AssetType to Money values
     * @param totalValue Total portfolio value for percentage calculations
     * @return AllocationResponse with formatted allocations and percentages
     */
    public static AllocationResponse toResponseFromAssetType(Map<AssetType, Money> allocation, Money totalValue, Instant asOfDate) {
        Instant responseDate = asOfDate != null ? asOfDate : Instant.now();
        if (allocation == null || allocation.isEmpty()) {
            return new AllocationResponse(
                new HashMap<>(),
                totalValue != null ? totalValue : new Money(BigDecimal.ZERO, null),
                responseDate
            );
        }
        
        // Convert AssetType keys to String keys
        Map<String, AllocationDetail> allocationDetails = allocation.entrySet().stream()
                .collect(Collectors.toMap(
                    entry -> entry.getKey().name(),
                    entry -> toAllocationDetailFromAssetType(entry.getKey(), entry.getValue(), totalValue)
                ));
        
        return new AllocationResponse(
            allocationDetails,
            totalValue != null ? totalValue : new Money(BigDecimal.ZERO, null),
            responseDate
        );
    }
    
    /**
     * Converts allocation data for a specific AssetType to an AllocationDetail.
     * Type-safe version that accepts AssetType enum directly.
     * 
     * @param assetType The AssetType enum
     * @param value The monetary value of this category
     * @param total The total value for percentage calculation
     * @return AllocationDetail with category, value, and calculated percentage
     */
    public static AllocationDetail toAllocationDetailFromAssetType(AssetType assetType, Money value, Money total) {
        if (assetType == null || value == null) {
            throw new IllegalArgumentException("AssetType and value cannot be null");
        }
        
        return new AllocationDetail(assetType, value, calculatePercentage(value, total));
    }
    
    /**
     * Calculates the percentage that a value represents of a total.
     * Handles edge cases like zero or null totals.
     * 
     * @param value The value to calculate percentage for
     * @param total The total value
     * @return Percentage object representing (value / total) * 100
     */
    private static Percentage calculatePercentage(Money value, Money total) {
        if (total == null || total.amount().compareTo(BigDecimal.ZERO) == 0) {
            return new Percentage(BigDecimal.ZERO);
        }
        
        if (value == null) {
            return new Percentage(BigDecimal.ZERO);
        }
        
        // Currency validation (Crucial for multi-currency portfolios)
        if (value.currency() != null && total.currency() != null && !value.currency().equals(total.currency())) {
             throw new IllegalArgumentException("Currency mismatch during allocation: " + value.currency() + " vs " + total.currency());
        }
        
        BigDecimal percentageValue = value.amount()
            .divide(total.amount(), Precision.DIVISION.getDecimalPlaces(), Rounding.DIVISION.getMode())
            .multiply(BigDecimal.valueOf(100));
        
        return new Percentage(percentageValue);
    }
}
