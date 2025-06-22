package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.math.BigDecimal;

public record InvestmentPerformanceSummary(Money totalGainLoss, Percentage totalReturnPercentage, Percentage annualizedReturn) {
    
}
