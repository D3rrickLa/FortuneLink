package com.laderrco.fortunelink.portfolio.infrastructure.market;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CachedMarketDataService implements MarketDataService {
  private final MarketDataService delegate; // the actual FMP client
  private final MarketAssetInfoRepository infoRepository;

  @Override
  public Map<AssetSymbol, MarketAssetQuote> getBatchQuotes(Set<AssetSymbol> symbols) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getBatchQuotes'");
  }

  @Override
  public Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getHistoricalQuote'");
  }

  @Override
  public Optional<MarketAssetInfo> getAssetInfo(AssetSymbol symbol) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getAssetInfo'");
  }

  @Override
  public Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(Set<AssetSymbol> symbols) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getBatchAssetInfo'");
  }

  @Override
  public Currency getTradingCurrency(AssetSymbol symbol) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getTradingCurrency'");
  }

  @Override
  public boolean isSymbolSupported(AssetSymbol symbol) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'isSymbolSupported'");
  }

}
