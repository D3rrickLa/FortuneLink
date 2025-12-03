package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AllocationResponse {
    private final Map<String, AllocationDetail> allocations;
    private final Money totalValue;
    private final Instant asOfDate;
    
    
    public AllocationResponse(Map<String, AllocationDetail> allocations, Money totalValue, Instant asOfDate) {
        this.allocations = allocations;
        this.totalValue = totalValue;
        this.asOfDate = asOfDate;
    }



    @Data
    @AllArgsConstructor
    public class AllocationDetail {
        private final AssetType category;
        private final Money value;
        private final Percentage percentage;   
    }
}