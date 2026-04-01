package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.MarketAssetInfoJpaEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MarketAssetInfoRepositoryImpl implements MarketAssetInfoRepository {
  private final JpaMarketAssetInfoRepository jpaRepo;

  @Value("${fortunelink.cache.ttl.asset-info}")
  private final long ttlSeconds;

  @Override
  public Optional<MarketAssetInfo> findBySymbol(AssetSymbol symbol) {
    return jpaRepo.findById(symbol.symbol())
        .map(MarketAssetInfoJpaEntity::toDomain);
  }

  @Override
  public Map<AssetSymbol, MarketAssetInfo> findBySymbols(Set<AssetSymbol> symbols) {
    Set<String> rawSymbols = symbols.stream()
        .map(AssetSymbol::symbol)
        .collect(Collectors.toSet());

    return jpaRepo.findBySymbolIn(rawSymbols).stream()
        .collect(Collectors.toMap(e -> new AssetSymbol(e.getSymbol()), MarketAssetInfoJpaEntity::toDomain));
  }

  @Override
  public void save(MarketAssetInfo info) {
    jpaRepo.save(MarketAssetInfoJpaEntity.from(info, ttlSeconds));
  }

  @Override
  public void saveAll(Map<AssetSymbol, MarketAssetInfo> infoMap) {
    List<MarketAssetInfoJpaEntity> entities = infoMap.values().stream()
        .map(info -> MarketAssetInfoJpaEntity.from(info, ttlSeconds))
        .toList();
    jpaRepo.saveAll(entities);
  }

  @Override
  public void deleteExpired() {
    jpaRepo.deleteExpired(Instant.now());
  }
}