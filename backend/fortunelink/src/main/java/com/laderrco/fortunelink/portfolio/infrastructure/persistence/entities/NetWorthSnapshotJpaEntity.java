package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.NetWorthSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Persistence model for {@code NetWorthSnapshot}.
 *
 * <p>
 * Append-only. Rows are never updated , one row per user per calendar day (enforced by the unique
 * index on (user_id, DATE(snapshot_date))).
 *
 * <p>
 * No @Version / optimistic locking needed because this entity is write-once from a single scheduled
 * job. If the job fires twice on the same day, the DB constraint prevents the second write , no
 * concurrent mutation scenario exists.
 */
@Entity
@Getter
@Table(name = "net_worth_snapshots")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED) // JPA
public class NetWorthSnapshotJpaEntity {

  @Id
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "total_assets_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal totalAssetsAmount;

  @Column(name = "total_assets_currency", nullable = false, length = 3)
  private String totalAssetsCurrency;

  @Column(name = "total_liabilities_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal totalLiabilitiesAmount;

  @Column(name = "total_liab_currency", nullable = false, length = 3)
  private String totalLiabCurrency;

  @Column(name = "net_worth_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal netWorthAmount;

  @Column(name = "net_worth_currency", nullable = false, length = 3)
  private String netWorthCurrency;

  @Column(name = "display_currency_code", nullable = false, length = 3)
  private String displayCurrencyCode;

  @Column(name = "has_stale_data", nullable = false)
  private boolean hasStaleData;

  @Column(name = "snapshot_date", nullable = false, updatable = false)
  private Instant snapshotDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // -------------------------------------------------------------------------
  // Factory
  // -------------------------------------------------------------------------

  public static NetWorthSnapshotJpaEntity from(NetWorthSnapshot domain) {
    NetWorthSnapshotJpaEntity e = new NetWorthSnapshotJpaEntity();
    e.id = domain.id();
    e.userId = UUID.fromString(domain.userId().toString());
    e.totalAssetsAmount = domain.totalAssets().amount();
    e.totalAssetsCurrency = domain.totalAssets().currency().getCode();
    e.totalLiabilitiesAmount = domain.totalLiabilities().amount();
    e.totalLiabCurrency = domain.totalLiabilities().currency().getCode();
    e.netWorthAmount = domain.netWorth().amount();
    e.netWorthCurrency = domain.netWorth().currency().getCode();
    e.displayCurrencyCode = domain.displayCurrency().getCode();
    e.hasStaleData = domain.hasStaleData();
    e.snapshotDate = domain.snapshotDate();
    e.createdAt = Instant.now();
    return e;
  }

  // -------------------------------------------------------------------------
  // toDomain
  // -------------------------------------------------------------------------

  public NetWorthSnapshot toDomain() {
    Currency displayCurrency = Currency.of(displayCurrencyCode);
    return new NetWorthSnapshot(id, UserId.fromString(userId.toString()),
        new Money(totalAssetsAmount, Currency.of(totalAssetsCurrency)),
        new Money(totalLiabilitiesAmount, Currency.of(totalLiabCurrency)),
        new Money(netWorthAmount, Currency.of(netWorthCurrency)), displayCurrency, hasStaleData,
        snapshotDate);
  }
}