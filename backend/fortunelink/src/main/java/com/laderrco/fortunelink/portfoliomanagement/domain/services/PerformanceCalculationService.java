package com.laderrco.fortunelink.portfoliomanagement.domain.services;

import java.util.List;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.entities.Transaction;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class PerformanceCalculationService {
    public Percentage calculateTotalReturn(Portfolio portfolio, MarketDataService marketDataService) {
        return null;
    }

    public Money calculateRealizedGains(List<Transaction> transactions) {
        return null;
    }

    public Money calculateUnrealizedGains(Portfolio portfolio, MarketDataService marketDataService) {
        return null;
    }

    public Percentage calculateTimeWeightReturn(Portfolio portfolio) {
        return null;
    }
}
