package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpSearchResponse;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;

@Component
public class FmpResponseMapper {
  /**
   * Quotes don't carry a currency field from FMP on the free tier.
   * Caller is responsible for passing the known trading currency
   * sourced from MarketAssetInfo (stored in market_asset_info table).
   * This eliminates the fragile exchange-name inference entirely.
   */
  public MarketAssetQuote toQuote(FmpQuoteResponse fmp, Currency tradingCurrency) {
    if (fmp == null)
      return null;

    return new MarketAssetQuote(
        new AssetSymbol(fmp.getSymbol()),
        Price.of(fmp.getPrice(), tradingCurrency),
        Price.of(fmp.getOpen(), tradingCurrency),
        Price.of(fmp.getDayHigh(), tradingCurrency),
        Price.of(fmp.getDayLow(), tradingCurrency),
        Price.of(fmp.getPreviousClose(), tradingCurrency),
        new PercentageChange(
            fmp.getChangePercentage()
                .divide(BigDecimal.valueOf(100),
                    Precision.PERCENTAGE.getDecimalPlaces(),
                    Rounding.PERCENTAGE.getMode())),
        fmp.getChange(),
        fmp.getMarketCap(),
        fmp.getVolume(),
        "FMP",
        Instant.ofEpochSecond(fmp.getTimestamp()));
  }

  public MarketAssetInfo toAssetInfo(FmpProfileResponse fmp) {
    if (fmp == null)
      return null;

    return new MarketAssetInfo(
        new AssetSymbol(fmp.getSymbol()),
        fmp.getCompanyName(),
        AssetType.valueOf(mapToAssetType(fmp)),
        fmp.getExchange(),
        Currency.of(fmp.getCurrency()),
        fmp.getSector(),
        fmp.getDescription());
  }

  public SymbolSearchResult toSearchResult(FmpSearchResponse fmp) {
    if (fmp == null)
      return null;

    return new SymbolSearchResult(
        new AssetSymbol(fmp.getSymbol()),
        fmp.getName(),
        fmp.getExchangeFullName(),
        Currency.of(fmp.getCurrency()));
  }

  private String mapToAssetType(FmpProfileResponse fmp) {
    if (Boolean.TRUE.equals(fmp.getIsEtf()))
      return "ETF";
    if (Boolean.TRUE.equals(fmp.getIsFund()))
      return "OTHER";

    if (fmp.getExchange() != null) {
      String ex = fmp.getExchange().toUpperCase();
      if (ex.contains("CRYPTO"))
        return "CRYPTO";
      if (ex.contains("FOREX") || ex.contains("CURRENCY"))
        return "FOREX_PAIR";
    }
    return "STOCK";
  }
}