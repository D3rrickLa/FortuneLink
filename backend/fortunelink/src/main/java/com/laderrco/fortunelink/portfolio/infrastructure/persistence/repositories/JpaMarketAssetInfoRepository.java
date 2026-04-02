package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.MarketAssetInfoJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaMarketAssetInfoRepository extends
    JpaRepository<MarketAssetInfoJpaEntity, String> {
  @Query("SELECT e FROM MarketAssetInfoJpaEntity e WHERE e.symbol IN :symbols")
  List<MarketAssetInfoJpaEntity> findBySymbolIn(@Param("symbols") Set<String> symbols);

  @Modifying
  @Query("DELETE FROM MarketAssetInfoJpaEntity e WHERE e.expiresAt < :now")
  int deleteExpired(@Param("now") Instant now);
}
