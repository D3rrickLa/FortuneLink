package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import java.time.Instant;

import org.springframework.data.annotation.Id;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "market_asset_info")
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

  // JPA-only constructor
  protected MarketAssetInfoJpaEntity() {
  }

  // ... getters, a static factory from(MarketAssetInfo), and a toDomain() method
  public MarketAssetInfo toDomainObject() {
    return new MarketAssetInfo(
        new AssetSymbol(symbol),
        name,
        AssetType.valueOf(assetType),
        exchange,
        Currency.of(tradingCurrency),
        sector,
        description);
  }
}