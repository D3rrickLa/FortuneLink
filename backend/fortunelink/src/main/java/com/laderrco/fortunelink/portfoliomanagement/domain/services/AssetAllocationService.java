package com.laderrco.fortunelink.portfoliomanagement.domain.services;

import java.util.Map;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AssetType;
import com.laderrco.fortunelink.shared.enums.Currency;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class AssetAllocationService {
    public Map<AssetType, Percentage> calcualteAllocationByType(Portfolio portfolio) {
        return null;
    }

    public Map<AccountType, Percentage> calculateAllocationByAccount(Portfolio portfolio) {
        return null;
    }

    public Map<Currency, Percentage> calcualteAllocationByCurrency(Portfolio portfolio) {
        return null;
    }
}
