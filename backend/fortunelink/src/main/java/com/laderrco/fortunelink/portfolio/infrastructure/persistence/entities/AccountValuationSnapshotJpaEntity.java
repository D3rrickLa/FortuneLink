package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "account_valuation_snapshots", uniqueConstraints = @UniqueConstraint(name = "uq_account_snapshot_date", columnNames = {
    "account_id", "snapshot_date" }))
public class AccountValuationSnapshotJpaEntity {
  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "account_id", nullable = false, updatable = false)
  private UUID accountId;

  @Column(name = "snapshot_date", nullable = false)
  private LocalDate snapshotDate;

  @Column(name = "snapshot_day", nullable = false)
  private LocalDate snapshotDay;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal totalValue;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal totalCostBasis;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal unrealizedGainLoss;

  @Column(nullable = false)
  private BigDecimal gainLossPercent;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal cashBalance;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal investedValue;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false)
  private boolean hasStaleData;
}
