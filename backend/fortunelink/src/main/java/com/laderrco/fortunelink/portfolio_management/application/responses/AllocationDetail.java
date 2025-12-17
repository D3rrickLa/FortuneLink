package com.laderrco.fortunelink.portfolio_management.application.responses;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllocationDetail {
    private final AssetType category;
    private final Money value;
    private final Percentage percentage;   
}