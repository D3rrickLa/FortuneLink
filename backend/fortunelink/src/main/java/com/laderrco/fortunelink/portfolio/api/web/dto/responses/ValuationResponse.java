package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import java.math.BigDecimal;

import com.laderrco.fortunelink.portfolio.application.views.ValuationView;

import io.swagger.v3.oas.annotations.media.Schema;
// TODO: replace Money with MoneyResponse
@Schema(description = "Current valuation of a portfolio")
public record ValuationResponse(

    @Schema(description = "Current market value of the portfolio") MoneyResponse totalValue,

    @Schema(description = "Total cost basis across all positions") MoneyResponse totalCostBasis,

    @Schema(description = "Unrealized gain/loss") MoneyResponse unrealizedGainLoss,

    @Schema(description = "Gain/loss percentage") BigDecimal gainLossPercent,

    @Schema(description = "Total cash balance across all accounts") MoneyResponse totalCashBalance,

    @Schema(description = "Market value of non-cash positions") MoneyResponse totalInvestedValue,

    @Schema(description = "ISO-4217 currency code") String currency,

    @Schema(description = "True when stale pricing data was used") boolean hasStaleData

) {

  public static ValuationResponse from(ValuationView view) {
    return new ValuationResponse(
        MoneyResponse.from(view.totalValue()),
        MoneyResponse.from(view.totalCostBasis()),
        MoneyResponse.from(view.unrealizedGainLoss()),
        view.gainLossPercent(),
        MoneyResponse.from(view.totalCashBalance()),
        MoneyResponse.from(view.totalInvestedValue()),
        view.displayCurrency().getCode(),
        view.hasStaleData());
  }
}