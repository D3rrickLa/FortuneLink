package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a ticker/symbol search")
public record SymbolSearchResponse(
    String symbol, String name, String exchange, String tradingCurrency) {

  public static SymbolSearchResponse fromDomain(SymbolSearchResult r) {
    return new SymbolSearchResponse(r.symbol().symbol(), r.name(), r.exchange(),
        r.tradingCurrency().getCode());
  }
}