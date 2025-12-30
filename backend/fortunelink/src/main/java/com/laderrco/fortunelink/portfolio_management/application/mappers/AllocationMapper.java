package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
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
        // Prevent instantiation
    }

    /**
     * Generic method to convert any allocation map to response
     * @param allocation Map of any type to Money
     * @param keyExtractor Function to extract string key from map key type
     * @param categoryType The type of category (e.g., "Asset Type", "Account Type", "Currency")
     * @param totalValue Total portfolio value
     * @param asOfDate Timestamp for the allocation
     * @return AllocationResponse with details and percentages
     */
    private static <T> AllocationResponse toResponse(
            Map<T, Money> allocation,
            Function<T, String> keyExtractor,
            Function<T, String> categoryNameExtractor,
            String categoryType,
            Money totalValue,
            Instant asOfDate) {
        
        Instant responseDate = asOfDate != null ? asOfDate : Instant.now();
        Money safeTotal = totalValue != null ? totalValue : Money.ZERO("USD");
        
        if (allocation == null || allocation.isEmpty()) {
            return new AllocationResponse(new HashMap<>(), safeTotal, responseDate);
        }
        
        Map<String, AllocationDetail> allocationDetails = allocation.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> keyExtractor.apply(entry.getKey()),
                entry -> createAllocationDetail(
                    categoryNameExtractor.apply(entry.getKey()),
                    categoryType,
                    entry.getValue(),
                    safeTotal
                )
            ));
        
        return new AllocationResponse(allocationDetails, safeTotal, responseDate);
    }

    /**
     * Creates an allocation detail with percentage calculation
     */
    private static AllocationDetail createAllocationDetail(
            String category,
            String categoryType,
            Money value,
            Money total) {
        
        if (category == null || categoryType == null || value == null) {
            throw new IllegalArgumentException("Category, category type and value cannot be null");
        }
        
        Percentage percentage = calculatePercentage(value, total);
        return new AllocationDetail(category, categoryType, value, percentage);
    }

    /**
     * Calculate percentage without currency validation
     * Used for all allocation types where values are already in base currency
     */
    private static Percentage calculatePercentage(Money value, Money total) {
        if (total == null || total.amount().compareTo(BigDecimal.ZERO) == 0) {
            return new Percentage(BigDecimal.ZERO);
        }
        
        if (value == null) {
            return new Percentage(BigDecimal.ZERO);
        }
        
        BigDecimal percentageValue = value.amount()
            .divide(total.amount(), Precision.DIVISION.getDecimalPlaces(), Rounding.DIVISION.getMode())
            .multiply(BigDecimal.valueOf(100));
        
        return new Percentage(percentageValue);
    }

    // ========== Public API Methods ==========

    /**
     * Type-safe method for AssetType allocations
     */
    public static AllocationResponse toResponseFromAssetType(
            Map<AssetType, Money> allocation,
            Money totalValue,
            Instant asOfDate) {
        
        return toResponse(
            allocation,
            AssetType::name,           // Key for map
            AssetType::name,           // Display name
            "Asset Type",
            totalValue,
            asOfDate
        );
    }

    /**
     * Type-safe method for AccountType allocations
     */
    public static AllocationResponse toResponseFromAccountType(
            Map<AccountType, Money> allocation,
            Money totalValue,
            Instant asOfDate) {
        
        return toResponse(
            allocation,
            AccountType::name,
            AccountType::name,
            "Account Type",
            totalValue,
            asOfDate
        );
    }

    /**
     * Type-safe method for Currency allocations
     */
    public static AllocationResponse toResponseFromCurrency(
            Map<ValidatedCurrency, Money> allocation,
            Money totalValue,
            Instant asOfDate) {
        
        return toResponse(
            allocation,
            ValidatedCurrency::getCode,
            ValidatedCurrency::getCode,
            "Currency",
            totalValue,
            asOfDate
        );
    }

    /**
     * @deprecated Use type-safe methods instead (toResponseFromAssetType, etc.)
     * This method assumes all allocations are AssetType
     */
    @Deprecated
    public static AllocationResponse toResponse(
            Map<String, Money> allocation,
            Money totalValue,
            Instant asOfDate) {
        
        return toResponse(
            allocation,
            Function.identity(),      // String key stays as-is
            Function.identity(),      // String name stays as-is
            "Asset Type",             // Default to Asset Type for backward compatibility
            totalValue,
            asOfDate
        );
    }
}