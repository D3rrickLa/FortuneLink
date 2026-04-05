package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpSearchResponse;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class FmpResponseMapper {
  /**
   * Quotes don't carry a currency field from FMP on the free tier. Caller is responsible for
   * passing the known trading currency sourced from MarketAssetInfo (stored in market_asset_info
   * table). This eliminates the fragile exchange-name inference entirely.
   */
  public MarketAssetQuote toQuote(FmpQuoteResponse fmp, Currency tradingCurrency) {
    if (fmp == null) {
      return null;
    }

    // Guard nullable numeric fields before constructing value objects
    BigDecimal changePercent = fmp.getChangePercentage() != null ? fmp.getChangePercentage()
        .divide(BigDecimal.valueOf(100), Precision.PERCENTAGE.getDecimalPlaces(),
            Rounding.PERCENTAGE.getMode()) : BigDecimal.ZERO;

    Instant timestamp =
        fmp.getTimestamp() != null ? Instant.ofEpochSecond(fmp.getTimestamp()) : Instant.now();

    return new MarketAssetQuote(new AssetSymbol(fmp.getSymbol()),
        priceOrZero(fmp.getPrice(), tradingCurrency), priceOrZero(fmp.getOpen(), tradingCurrency),
        priceOrZero(fmp.getDayHigh(), tradingCurrency),
        priceOrZero(fmp.getDayLow(), tradingCurrency),
        priceOrZero(fmp.getPreviousClose(), tradingCurrency), new PercentageChange(changePercent),
        Objects.requireNonNullElse(fmp.getChange(), BigDecimal.ZERO), fmp.getMarketCap(),
        // nullable on MarketAssetQuote is fine, just BigDecimal
        fmp.getVolume(), "FMP", timestamp);
  }

  public MarketAssetInfo toAssetInfo(FmpProfileResponse fmp) {
    if (fmp == null) {
      return null;
    }

    return new MarketAssetInfo(new AssetSymbol(fmp.getSymbol()), fmp.getCompanyName(),
        AssetType.valueOf(mapToAssetType(fmp)), fmp.getExchange(), Currency.of(fmp.getCurrency()),
        fmp.getSector(), fmp.getDescription());
  }

  public SymbolSearchResult toSearchResult(FmpSearchResponse fmp) {
    if (fmp == null) {
      return null;
    }

    return new SymbolSearchResult(new AssetSymbol(fmp.getSymbol()), fmp.getName(),
        fmp.getExchangeFullName(), Currency.of(fmp.getCurrency()));
  }

  private String mapToAssetType(FmpProfileResponse fmp) {
    if (Boolean.TRUE.equals(fmp.getIsEtf())) {
      return "ETF";
    }
    if (Boolean.TRUE.equals(fmp.getIsFund())) {
      return "OTHER";
    }

    if (fmp.getExchange() != null) {
      String ex = fmp.getExchange().toUpperCase();
      if (ex.contains("CRYPTO")) {
        return "CRYPTO";
      }
      if (ex.contains("FOREX") || ex.contains("CURRENCY")) {
        return "FOREX_PAIR";
      }
    }
    return "STOCK";
  }

  private Price priceOrZero(BigDecimal amount, Currency currency) {
    return amount != null ? Price.of(amount, currency) : Price.zero(currency);
  }
}