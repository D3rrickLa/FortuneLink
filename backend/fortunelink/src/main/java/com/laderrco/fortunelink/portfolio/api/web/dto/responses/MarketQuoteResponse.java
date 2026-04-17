package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Real-time or cached market price data")
public record MarketQuoteResponse(
    String symbol, double currentPrice, double openPrice, double highPrice, 
    double lowPrice, double previousClose, double changePercent, 
    double changeAmount, String currency, Instant timestamp) {
  public static MarketQuoteResponse fromDomain(MarketAssetQuote q) {
    return new MarketQuoteResponse(q.symbol().symbol(), q.currentPrice().amount().doubleValue(),
        q.openPrice() != null ? q.openPrice().amount().doubleValue() : 0,
        q.highPrice() != null ? q.highPrice().amount().doubleValue() : 0,
        q.lowPrice() != null ? q.lowPrice().amount().doubleValue() : 0,
        q.previousClose() != null ? q.previousClose().amount().doubleValue() : 0,
        q.changePercent() != null ? q.changePercent().toPercent().doubleValue() : 0,
        q.changeAmount() != null ? q.changeAmount().doubleValue() : 0,
        q.currentPrice().currency().getCode(), q.timestamp());
  }
}