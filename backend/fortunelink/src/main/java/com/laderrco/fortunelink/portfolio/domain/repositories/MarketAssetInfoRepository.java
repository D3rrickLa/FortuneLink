package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MarketAssetInfoRepository {
  Optional<MarketAssetInfo> findBySymbol(AssetSymbol symbol);

  Map<AssetSymbol, MarketAssetInfo> findBySymbols(Set<AssetSymbol> symbols);

  void save(MarketAssetInfo info);

  void saveAll(Map<AssetSymbol, MarketAssetInfo> infoMap);

  void deleteExpired(); // for the scheduled cleanup job
}