package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import java.math.BigDecimal;

public record AccountValuationView(
    Money totalValue,
    Money totalCostBasis,
    Money unrealizedGainLoss,
    BigDecimal gainLossPercent,
    Money cashBalance,
    Money investedValue,
    Currency currency) {
}