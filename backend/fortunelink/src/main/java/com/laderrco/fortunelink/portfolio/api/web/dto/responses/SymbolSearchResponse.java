package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.SymbolSearchResult;

public record SymbolSearchResponse(
      String symbol,
      String name,
      String exchange,
      String tradingCurrency) {

    public static SymbolSearchResponse fromDomain(SymbolSearchResult r) {
      return new SymbolSearchResponse(
          r.symbol().symbol(),
          r.name(),
          r.exchange(),
          r.tradingCurrency().getCode());
    }
  }