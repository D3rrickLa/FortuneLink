package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpSearchResponse;

@Component
public class FmpResponseMapper {
  public MarketAssetQuote toDomain(FmpQuoteResponse fmp) {
    if (fmp == null)
      return null;

    Currency currency = Currency.of(inferFromExchange(fmp.getExchange()));
    return new MarketAssetQuote(
        new AssetSymbol(fmp.getSymbol()), // Assuming AssetSymbol is a value object
        Price.of(fmp.getPrice(), currency),
        Price.of(fmp.getOpen(), currency),
        Price.of(fmp.getDayHigh(), currency),
        Price.of(fmp.getDayLow(), currency),
        Price.of(fmp.getPreviousClose(), currency),
        new PercentageChange(fmp.getChangePercentage()),
        null,
        fmp.getMarketCap(),
        fmp.getVolume(),
        "Financial Modeling Prep",
        Instant.ofEpochSecond(fmp.getTimestamp()));
  }

  public MarketAssetInfo toDomain(FmpProfileResponse fmp) {
    if (fmp == null)
      return null;

    return new MarketAssetInfo(
        new AssetSymbol(fmp.getSymbol()),
        fmp.getCompanyName(),
        AssetType.valueOf(mapQuoteTypeToAssetType(fmp)),
        fmp.getExchange(),
        Currency.of(fmp.getCountry()),
        fmp.getSector(),
        fmp.getDescription());
  }

  public MarketAssetInfo toDomain(FmpSearchResponse fmp) {
    if (fmp == null) {
      return null;
    }

    return new MarketAssetInfo(
      new AssetSymbol(fmp.getSymbol()),
      fmp.getName(),
      null,
      fmp.getExchangeFullName(),
      Currency.of(fmp.getCurrency()),
      null,
      null
    );
  }

  private String mapQuoteTypeToAssetType(FmpProfileResponse profileResponse) {
    if (profileResponse.getIsEtf()) {
      return "ETF";
    }

    if (profileResponse.getIsFund()) {
      return "MUTUAL_FUND";
    }

    if (profileResponse.getExchange() != null) {
      String ex = profileResponse.getExchange().toUpperCase();
      if (ex.contains("CRYPTO"))
        return "CRYPTO";
      if (ex.contains("FOREX") || ex.contains("CURRENCY"))
        return "CURRENCY";
    }
    // 3. Check Industry/Sector for Indices
    if (profileResponse.getIndustry() != null && profileResponse.getIndustry().toUpperCase().contains("INDEX")) {
      return "INDEX";
    }

    return "STOCK";
  }

  private String inferFromExchange(String exchange) {
    if (exchange == null) {
      return "USD";
    }

    String ex = exchange.toUpperCase();

    // direct class match - REALLY DIRTY
    if (ex.contains("CRYPTO"))
      return "USD";
    if (ex.contains("FOREX"))
      return "USD";
    if (ex.contains("COMMODITY"))
      return "USD";
    if (ex.contains("INDEX"))
      return "USD";

    return switch (ex) {
      case "TSX", "TORONTO", "TSXV" -> "CAD";
      case "LSE", "LONDON" -> "GBP";
      case "XETRA", "EURONEXT", "MCX" -> "EUR";
      case "HKSE" -> "HKD";
      case "ASX" -> "AUD";
      case "NSE", "BSE" -> "INR";
      case "JPX", "TOKYO" -> "JPY";
      // Add more as needed
      default -> "USD"; // Default for NASDAQ/NYSE
    };
  }
}