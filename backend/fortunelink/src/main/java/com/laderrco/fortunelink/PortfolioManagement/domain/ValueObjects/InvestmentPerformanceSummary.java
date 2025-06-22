package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;

public record InvestmentPerformanceSummary(Money totalGainLoss, Percentage totalReturnPercentage, Percentage annualizedReturn) {
    public InvestmentPerformanceSummary {
        Objects.requireNonNull(totalGainLoss, "Total Gain/Loss cannot be null.");
        Objects.requireNonNull(totalReturnPercentage, "Total return percentage cannot be null.");
        Objects.requireNonNull(annualizedReturn, "Annualized return cannot be null.");
    }
}
