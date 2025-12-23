package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.Value;

@Value
public class AllocationResponse {
    private final Map<String, AllocationDetail> allocations;
    private final Money totalValue;
    private final Instant asOfDate;
    
    public AllocationResponse(Map<String, AllocationDetail> allocations, Money totalValue, Instant asOfDate) {
        Objects.requireNonNull(allocations);
        Objects.requireNonNull(totalValue);
        Objects.requireNonNull(asOfDate);
        this.allocations = allocations;
        this.totalValue = totalValue;
        this.asOfDate = asOfDate;
    }
}