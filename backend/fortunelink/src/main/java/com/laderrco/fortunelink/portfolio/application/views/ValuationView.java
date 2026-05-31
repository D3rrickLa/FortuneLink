package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;

@Builder
public record ValuationView(
    Money totalValue,
    Money totalCostBasis,
    Money unrealizedGainLoss,
    BigDecimal gainLossPercent,
    Money totalCashBalance,
    Money totalInvestedValue,
    Currency displayCurrency,
    boolean hasStaleData,
    Instant asOfDate) {

  public static ValuationView of(Money totalValue, Money totalCostBasis, Money totalCashBalance,
      Money totalInvestedValue, Currency displayCurrency, boolean hasStaleData, Instant asOfDate) {
    Money unrealizedGainLoss = totalValue.subtract(totalCostBasis);
    BigDecimal gainLossPercent = computePercent(unrealizedGainLoss, totalCostBasis);

    return new ValuationView(totalValue, totalCostBasis, unrealizedGainLoss, gainLossPercent,
        totalCashBalance, totalInvestedValue, displayCurrency, hasStaleData, asOfDate);
  }

  private static BigDecimal computePercent(Money numerator, Money denominator) {
    if (denominator == null || denominator.isZero()) {
      return BigDecimal.ZERO;
    }

    return numerator.amount().divide(denominator.amount(), Precision.DIVISION.getDecimalPlaces(),
        Rounding.DIVISION.getMode()).multiply(BigDecimal.valueOf(100))
        .setScale(Precision.DIVISION.getDecimalPlaces(), Rounding.DIVISION.getMode());
  }

  public static ValuationView empty(Currency reportingCurrency) {
    return new ValuationView(
        Money.zero(reportingCurrency),
        Money.zero(reportingCurrency),
        Money.zero(reportingCurrency),
        BigDecimal.ZERO,
        Money.zero(reportingCurrency),
        Money.zero(reportingCurrency),
        reportingCurrency,
        false,
        Instant.now());
  }
}