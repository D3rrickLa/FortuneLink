package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

// organizes data
// it's a snapshot object that answers the question - How is my money distributed across different invesetments right now?
// in simple terms: How much Apple stock is my portfolio
// NOTE: this needs to somehow handle currency conversion if we have a different currency preference
public final class AssetAllocation {
    private final Money totalValue;
    private final Currency baseCurrency;
    private final LocalDateTime calculatedAt;
    private final Map<String, AllocationItem> allocationsBySymbol;
    private final Map<AssetType, AllocationItem> allocationsByType;

    public AssetAllocation(Money totalValue, Currency baseCurrency) {
        Objects.requireNonNull(totalValue, "Total value cannot be null.");
        Objects.requireNonNull(baseCurrency, "Base currency cannot be null.");

        this.totalValue = totalValue;
        this.baseCurrency = baseCurrency;
        this.allocationsBySymbol = new HashMap<>();
        this.allocationsByType = new HashMap<>();
        this.calculatedAt = LocalDateTime.now();
    }

    public void addAllocation(AssetIdentifier assetIdentifier, Money value, Percentage percentage) {
        Objects.requireNonNull(assetIdentifier, "Asset identifier cannot be null.");
        Objects.requireNonNull(value, "Value cannot be null.");
        Objects.requireNonNull(percentage, "Percentage cannot be null.");
        
        String symbol = assetIdentifier.symbol();
        AllocationItem item = new AllocationItem(assetIdentifier, value, percentage);
        allocationsBySymbol.put(symbol, item);

        // Aggregate by asset type
        AllocationItem typeItem = allocationsByType.get(assetIdentifier.assetType());

        if (typeItem == null) {
            allocationsByType.put(assetIdentifier.assetType(), new AllocationItem(assetIdentifier, value, percentage));
        }
        else {
            Money newValue = typeItem.value().add(value);
            BigDecimal percent = typeItem.percentage().toDecimal().add(percentage.toDecimal());
            Percentage newPercentage = new Percentage(percent);
            allocationsByType.put(assetIdentifier.assetType(), new AllocationItem(
                assetIdentifier, 
                newValue, 
                newPercentage
            ));
        }
    }

    
    public Percentage getPercentageBySymbol(String symbol) {
        AllocationItem item = allocationsBySymbol.get(symbol);
        return item != null ? item.percentage() : new Percentage(BigDecimal.ZERO);
    }

    public Percentage getPercentageByType(AssetType type) {
        AllocationItem item = allocationsByType.get(type);
        return item != null ? item.percentage() : new Percentage(BigDecimal.ZERO);
    }

    
    public boolean isDiversified(Percentage maxAllocationPercentage) {
        return allocationsBySymbol.values().stream()
            .noneMatch(item -> item.percentage().compareTo(maxAllocationPercentage) > 0);
    }
    
    public List<AllocationItem> getTopAllocations(int n) {
        return allocationsBySymbol.values().stream()
            .sorted((a, b) -> b.percentage().compareTo(a.percentage()))
            .limit(n)
            .collect(Collectors.toList());
    }
    
    public Money getTotalValue() {return totalValue;}
    public LocalDateTime getCalculatedAt() {return calculatedAt;} 
    public Currency getBaseCurrency() {return baseCurrency;}
    
    public Map<String, AllocationItem> getAllocationsBySymbol() {
        return Collections.unmodifiableMap(allocationsBySymbol);
    }
    
    public Map<AssetType, AllocationItem> getAllocationsByType() {
        return Collections.unmodifiableMap(allocationsByType);
    }
}
