package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.application.views.RealizedGainsSummaryView;
import java.util.List;

/**
 * API response for the realized gains summary endpoint.
 * <p>
 * Contains both the line-item breakdown (items) and pre-computed totals. The totals are
 * authoritative — the frontend must not re-sum items, as rounding on individual BigDecimal rows can
 * drift from the server total.
 * <p>
 * Tax year timezone caveat: All timestamps are UTC. For CRA (Canada) reporting, tax year runs Jan 1
 * – Dec 31 in the user's local timezone. If the user is in EST (UTC-5), a trade settled at
 * 2025-12-31T23:30Z is actually 2026-01-01 local time. The frontend must apply timezone offset
 * before presenting year-grouped totals. The taxYear field in this response reflects the
 * server-side UTC bucketing.
 * <p>
 * Currency note: All monetary values in this response are in the account's base currency.
 * Cross-currency gains are already converted at transaction recording time using the ExchangeRate
 * applied when the trade was executed.
 */
public record RealizedGainsSummaryResponse(
    List<RealizedGainItemResponse> items,
    MoneyResponse totalGains,
    MoneyResponse totalLosses,
    MoneyResponse netGainLoss,
    String currency,
    Integer taxYear,
    int itemCount) {

  public static RealizedGainsSummaryResponse from(RealizedGainsSummaryView view) {
    List<RealizedGainItemResponse> items = view.items().stream().map(
        item -> new RealizedGainItemResponse(item.symbol(),
            MoneyResponse.from(item.realizedGainLoss()), MoneyResponse.from(item.costBasisSold()),
            item.occurredAt(), item.isGain(),
            !item.isGain() && item.realizedGainLoss().isNegative())).toList();

    return new RealizedGainsSummaryResponse(items, MoneyResponse.from(view.totalGains()),
        MoneyResponse.from(view.totalLosses()), MoneyResponse.from(view.netGainLoss()),
        view.currency().getCode(), view.taxYear(), items.size());
  }
}
