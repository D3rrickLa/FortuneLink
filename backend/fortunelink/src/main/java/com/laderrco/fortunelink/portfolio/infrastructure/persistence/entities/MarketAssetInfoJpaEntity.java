package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "market_asset_info")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED) // for JPA
public class MarketAssetInfoJpaEntity {

  @Id
  @Column(name = "symbol", length = 20)
  private String symbol;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "asset_type", nullable = false)
  private String assetType;

  @Column(name = "exchange")
  private String exchange;

  @Column(name = "trading_currency", length = 3, nullable = false)
  private String tradingCurrency;

  @Column(name = "sector")
  private String sector;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "fetched_at", nullable = false)
  private Instant fetchedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Version
  @Column(name = "version", nullable = false)
  private int version;

  public MarketAssetInfo toDomain() {
    return new MarketAssetInfo(
        new AssetSymbol(symbol),
        name,
        AssetType.valueOf(assetType),
        exchange,
        Currency.of(tradingCurrency),
        sector,
        description);
  }

  public static MarketAssetInfoJpaEntity from(MarketAssetInfo info, long ttlSeconds) {
    MarketAssetInfoJpaEntity e = new MarketAssetInfoJpaEntity();
    e.symbol = info.symbol().symbol();
    e.name = info.name();
    e.assetType = info.type().name();
    e.exchange = info.exchange();
    e.tradingCurrency = info.tradingCurrency().getCode();
    e.sector = info.sector();
    e.description = info.description();
    e.fetchedAt = Instant.now();
    e.expiresAt = Instant.now().plusSeconds(ttlSeconds);
    return e;
  }
}