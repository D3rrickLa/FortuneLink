package com.laderrco.fortunelink.portfolio_management.application.responses;

import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
// had to change it to Strings because of how this is used with the allocation mapper
public class AllocationDetail {
    private final String category;  // "STOCK", "TFSA", "USD", etc.
    private final String categoryType;  // "Asset Type", "Account Type", "Currency"
    private final Money value;
    private final Percentage percentage;   
}