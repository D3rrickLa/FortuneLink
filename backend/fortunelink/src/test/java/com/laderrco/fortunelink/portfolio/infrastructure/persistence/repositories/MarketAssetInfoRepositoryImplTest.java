package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.MarketAssetInfoJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketAssetInfoRepositoryImpl Unit Tests")
class MarketAssetInfoRepositoryImplTest {

  @Mock
  private JpaMarketAssetInfoRepository jpaRepo;

  @InjectMocks
  private MarketAssetInfoRepositoryImpl repository;

  private static final String BTC_TICKER = "BTC";
  private static final String ETH_TICKER = "ETH";
  private static final long TTL_SECONDS = 3600L;

  @BeforeEach
  void setUp() {
    
    ReflectionTestUtils.setField(repository, "ttlSeconds", TTL_SECONDS);
  }

  @Nested
  @DisplayName("Finding Assets")
  class FindingAssets {

    @Test
    @DisplayName("findBySymbol should return domain object when entity exists")
    void findBySymbolShouldReturnDomainObjectWhenEntityExists() {
      AssetSymbol symbol = createSymbol(BTC_TICKER);
      MarketAssetInfoJpaEntity entity = createEntity(BTC_TICKER);
      when(jpaRepo.findById(BTC_TICKER)).thenReturn(Optional.of(entity));

      Optional<MarketAssetInfo> result = repository.findBySymbol(symbol);

      assertThat(result).isPresent();
      assertThat(result.get().symbol().symbol()).isEqualTo(BTC_TICKER);
      verify(jpaRepo).findById(BTC_TICKER);
    }

    @Test
    @DisplayName("findBySymbols should map multiple entities to a map")
    void findBySymbolsShouldMapMultipleEntitiesToAMap() {
      Set<AssetSymbol> symbols = Set.of(createSymbol(BTC_TICKER), createSymbol(ETH_TICKER));
      List<MarketAssetInfoJpaEntity> entities = List.of(createEntity(BTC_TICKER), createEntity(ETH_TICKER));

      when(jpaRepo.findBySymbolIn(anySet())).thenReturn(entities);

      Map<AssetSymbol, MarketAssetInfo> result = repository.findBySymbols(symbols);

      assertThat(result).hasSize(2);
      assertThat(result).containsKey(createSymbol(BTC_TICKER));
      assertThat(result).containsKey(createSymbol(ETH_TICKER));
    }
  }

  @Nested
  @DisplayName("Saving Assets")
  class SavingAssets {

    @Test
    @DisplayName("save should convert domain to entity with TTL")
    void saveShouldConvertDomainToEntityWithTtl() {
      MarketAssetInfo domain = new MarketAssetInfo(new AssetSymbol(BTC_TICKER), "Bitcoin", AssetType.STOCK, "Exchange",
          Currency.CAD, "Technology", "Desc");

      repository.save(domain);

      ArgumentCaptor<MarketAssetInfoJpaEntity> captor = ArgumentCaptor.forClass(MarketAssetInfoJpaEntity.class);
      verify(jpaRepo).save(captor.capture());

      
      
      assertThat(captor.getValue().getSymbol()).isEqualTo(BTC_TICKER);
    }

    @Test
    @DisplayName("saveAll should persist multiple entities")
    void saveAllShouldPersistMultipleEntities() {
      MarketAssetInfo info1 = new MarketAssetInfo(new AssetSymbol(BTC_TICKER), "Bitcoin", AssetType.STOCK, "Exchange",
          Currency.CAD, "Technology", "Desc");
      MarketAssetInfo info2 = new MarketAssetInfo(new AssetSymbol(ETH_TICKER), "Etherium", AssetType.STOCK, "Exchange",
          Currency.CAD, "Technology", "Desc");

      Map<AssetSymbol, MarketAssetInfo> infoMap = Map.of(
          createSymbol(BTC_TICKER), info1,
          createSymbol(ETH_TICKER), info2);

      repository.saveAll(infoMap);

      verify(jpaRepo).saveAll(anyList());
    }
  }

  @Nested
  @DisplayName("Cleanup Operations")
  class CleanupOperations {

    @Test
    @DisplayName("deleteExpired should call jpaRepo with current timestamp")
    void deleteExpiredShouldCallJpaRepo() {
      repository.deleteExpired();

      verify(jpaRepo).deleteExpired(any(Instant.class));
    }
  }

  

  private AssetSymbol createSymbol(String ticker) {
    return new AssetSymbol(ticker);
  }

  private MarketAssetInfoJpaEntity createEntity(String ticker) {
    MarketAssetInfoJpaEntity entity = MarketAssetInfoJpaEntity.from(
        new MarketAssetInfo(new AssetSymbol(ticker), "asset name", AssetType.STOCK, ticker, Currency.CAD, "Technology",
            "Desc"),
        TTL_SECONDS);
    entity.setSymbol(ticker);
    
    return entity;
  }
}