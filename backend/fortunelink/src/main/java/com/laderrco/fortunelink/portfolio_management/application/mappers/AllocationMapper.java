package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationDetail;
import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class AllocationMapper {

    private AllocationMapper() {
        // to prevent instantiation 
    }

    // Existing method for backward compatibility (String-based)
    // NOTE: This assumes all allocations are AssetType - consider deprecating this method
    @Deprecated
    public static AllocationResponse toResponse(Map<String, Money> allocation, Money totalValue, Instant asOfDate) {
        Instant responseDate = asOfDate != null ? asOfDate : Instant.now();

        if (allocation == null || allocation.isEmpty()) {
            return new AllocationResponse(
                new HashMap<>(),
                totalValue != null ? totalValue : Money.ZERO("USD"), 
                responseDate
            );
        }

        Map<String, AllocationDetail> allocationDetails = allocation.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                // Pass "Asset Type" as default category type for backward compatibility
                entry -> toAllocationDetail(entry.getKey(), "Asset Type", entry.getValue(), totalValue)
            ));
        
        return new AllocationResponse(
            allocationDetails, 
            totalValue != null ? totalValue : Money.ZERO("USD"), 
            responseDate
        );
    }
    
    private static AllocationDetail toAllocationDetail(String category, String categoryType, Money value, Money total) {
        if (category == null || categoryType == null || value == null) {
            throw new IllegalArgumentException("Category, category type and value cannot be null");
        }
        
        return new AllocationDetail(category, categoryType, value, calculatePercentageForCurrency(value, total));
    }
    
    /**
     * Type-safe method for AssetType allocations
     */
    public static AllocationResponse toResponseFromAssetType(Map<AssetType, Money> allocation, Money totalValue, Instant asOfDate) {
        Instant responseDate = asOfDate != null ? asOfDate : Instant.now();
        
        if (allocation == null || allocation.isEmpty()) {
            return new AllocationResponse(
                new HashMap<>(),
                totalValue != null ? totalValue : Money.ZERO("USD"),
                responseDate
            );
        }
        
        Map<String, AllocationDetail> allocationDetails = allocation.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().name(),
                entry -> toAllocationDetailFromAssetType(entry.getKey(), entry.getValue(), totalValue)
            ));
        
        return new AllocationResponse(
            allocationDetails,
            totalValue != null ? totalValue : Money.ZERO("USD"),
            responseDate
        );
    }
    
    private static AllocationDetail toAllocationDetailFromAssetType(AssetType assetType, Money value, Money total) {
        if (assetType == null || value == null) {
            throw new IllegalArgumentException("AssetType and value cannot be null");
        }
        
        return new AllocationDetail(assetType.name(), "Asset Type", value, calculatePercentageForCurrency(value, total));
    }
    
    /**
     * Type-safe method for AccountType allocations
     */
    public static AllocationResponse toResponseFromAccountType(Map<AccountType, Money> allocation, Money totalValue, Instant asOfDate) {
        Instant responseDate = asOfDate != null ? asOfDate : Instant.now();
        
        if (allocation == null || allocation.isEmpty()) {
            return new AllocationResponse(
                new HashMap<>(),
                totalValue != null ? totalValue : Money.ZERO("USD"),
                responseDate
            );
        }
        
        Map<String, AllocationDetail> allocationDetails = allocation.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().name(),
                entry -> toAllocationDetailFromAccountType(entry.getKey(), entry.getValue(), totalValue)
            ));
        
        return new AllocationResponse(
            allocationDetails,
            totalValue != null ? totalValue : Money.ZERO("USD"),
            responseDate
        );
    }
    
    private static AllocationDetail toAllocationDetailFromAccountType(AccountType accountType, Money value, Money total) {
        if (accountType == null || value == null) {
            throw new IllegalArgumentException("AccountType and value cannot be null");
        }
        
        return new AllocationDetail(accountType.name(), "Account Type", value, calculatePercentageForCurrency(value, total));
    }
    
    /**
     * Type-safe method for Currency allocations
     */
    public static AllocationResponse toResponseFromCurrency(Map<ValidatedCurrency, Money> allocation, Money totalValue, Instant asOfDate) {
        Instant responseDate = asOfDate != null ? asOfDate : Instant.now();
        
        if (allocation == null || allocation.isEmpty()) {
            return new AllocationResponse(
                new HashMap<>(),
                totalValue != null ? totalValue : Money.ZERO("USD"),
                responseDate
            );
        }
        
        Map<String, AllocationDetail> allocationDetails = allocation.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().getCode(),
                entry -> toAllocationDetailFromCurrency(entry.getKey(), entry.getValue(), totalValue)
            ));
        
        return new AllocationResponse(
            allocationDetails,
            totalValue != null ? totalValue : Money.ZERO("USD"),
            responseDate
        );
    }
    
    private static AllocationDetail toAllocationDetailFromCurrency(ValidatedCurrency currency, Money value, Money total) {
        if (currency == null || value == null) {
            throw new IllegalArgumentException("Currency and value cannot be null");
        }
        
        return new AllocationDetail(currency.getCode(), "Currency", value, calculatePercentageForCurrency(value, total));
    }
    
    /**
     * Calculate percentage without currency validation (used for currency allocations)
     */
    private static Percentage calculatePercentageForCurrency(Money value, Money total) {
        if (total == null || total.amount().compareTo(BigDecimal.ZERO) == 0) {
            return new Percentage(BigDecimal.ZERO);
        }
        
        if (value == null) {
            return new Percentage(BigDecimal.ZERO);
        }
        
        // No currency validation - assume values are already converted to base currency
        BigDecimal percentageValue = value.amount()
            .divide(total.amount(), Precision.DIVISION.getDecimalPlaces(), Rounding.DIVISION.getMode())
            .multiply(BigDecimal.valueOf(100));
        
        return new Percentage(percentageValue);
    }
}