package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed asset metadata including sector and exchange information")
public record AssetInfoResponse(
    String symbol, String name, String assetType, String exchange, 
    String tradingCurrency, String sector, String description) {

  public static AssetInfoResponse fromDomain(MarketAssetInfo info) {
    return new AssetInfoResponse(info.symbol().symbol(), info.name(), info.type().name(),
        info.exchange(), info.tradingCurrency().getCode(), info.sector(), info.description());
  }
}