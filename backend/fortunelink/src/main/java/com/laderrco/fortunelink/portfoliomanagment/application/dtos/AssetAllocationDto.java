package com.laderrco.fortunelink.portfoliomanagment.application.dtos;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;

public record AssetAllocationDto(
    UUID portfolioId,
    List<AllocationSegment> byAssetType,
    List<AllocationSegment> byIndustrySector,
    Money totalAllocatedValue
) {
    public AssetAllocationDto {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(byAssetType, "Asset type allocation cannot be null.");
        Objects.requireNonNull(byIndustrySector, "Industry sector allocation cannot be null.");
        Objects.requireNonNull(totalAllocatedValue, "Total allocated value cannot be null.");
    }

    public record AllocationSegment(
        String category,
        Money value,
        Percentage percentage
    ) {
        public AllocationSegment {
            Objects.requireNonNull(category, "Category cannot be null.");
            Objects.requireNonNull(value, "Value cannot be null.");
            Objects.requireNonNull(percentage, "Percentage cannot be null.");
        }
    }
}
