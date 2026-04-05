package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import java.util.List;

/**
 * NOTE: occurredAt timestamps are stored and returned in UTC. For tax year calculations, callers
 * should apply user timezone offset before bucketing by year. A Dec 31 23:00 UTC sale may be Jan 1
 * in some timezones. This is not handled server-side in MVP. Integer taxYear, null = all years
 */
public record RealizedGainsSummaryView(
    List<RealizedGainView> items,
    Money totalGains,
    Money totalLosses,
    Money netGainLoss,
    Currency currency,
    Integer taxYear) {
}