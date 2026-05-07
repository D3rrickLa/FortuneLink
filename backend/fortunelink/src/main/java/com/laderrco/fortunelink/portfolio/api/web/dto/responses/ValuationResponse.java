package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import java.math.BigDecimal;

import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current valuation of a portfolio")
public record ValuationResponse(

    @Schema(description = "Current market value of the portfolio") Money totalValue,

    @Schema(description = "Total cost basis across all positions") Money totalCostBasis,

    @Schema(description = "Unrealized gain/loss") Money unrealizedGainLoss,

    @Schema(description = "Gain/loss percentage") BigDecimal gainLossPercent,

    @Schema(description = "Total cash balance across all accounts") Money totalCashBalance,

    @Schema(description = "Market value of non-cash positions") Money totalInvestedValue,

    @Schema(description = "ISO-4217 currency code") String currency,

    @Schema(description = "True when stale pricing data was used") boolean hasStaleData

) {

  public static ValuationResponse from(ValuationView view) {
    return new ValuationResponse(
        view.totalValue(),
        view.totalCostBasis(),
        view.unrealizedGainLoss(),
        view.gainLossPercent(),
        view.totalCashBalance(),
        view.totalInvestedValue(),
        view.displayCurrency().getCode(),
        view.hasStaleData());
  }
}