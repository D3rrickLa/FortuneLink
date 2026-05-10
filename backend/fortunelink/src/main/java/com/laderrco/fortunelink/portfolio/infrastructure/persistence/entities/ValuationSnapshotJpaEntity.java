package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ValuationSnapshot;
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

@Entity
@Getter
@Table(name = "portfolio_valuation_snapshots")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class ValuationSnapshotJpaEntity {

  @Id
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  // ─────────────────────────────────────────────────────────────
  // Total Net Worth
  // ─────────────────────────────────────────────────────────────

  @Column(name = "total_value_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal totalValueAmount; // netWorth (total value) = assets - liabilities

  @Column(name = "total_value_currency", nullable = false, length = 3)
  private String totalValueCurrency;

  // ─────────────────────────────────────────────────────────────
  // Cost Basis
  // ─────────────────────────────────────────────────────────────

  @Column(name = "total_cost_basis_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal totalCostBasisAmount;

  @Column(name = "total_cost_basis_currency", nullable = false, length = 3)
  private String totalCostBasisCurrency;

  // ─────────────────────────────────────────────────────────────
  // Unrealized Gain/Loss
  // ─────────────────────────────────────────────────────────────

  @Column(name = "unrealized_gain_loss_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal unrealizedGainLossAmount;

  @Column(name = "unrealized_gain_loss_currency", nullable = false, length = 3)
  private String unrealizedGainLossCurrency;

  @Column(name = "gain_loss_percent", precision = 12, scale = 4)
  private BigDecimal gainLossPercent;

  // ─────────────────────────────────────────────────────────────
  // Cash Balance
  // ─────────────────────────────────────────────────────────────

  @Column(name = "total_cash_balance_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal totalCashBalanceAmount;

  @Column(name = "total_cash_balance_currency", nullable = false, length = 3)
  private String totalCashBalanceCurrency;

  // ─────────────────────────────────────────────────────────────
  // Invested Value
  // ─────────────────────────────────────────────────────────────

  @Column(name = "total_invested_value_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal totalInvestedValueAmount;

  @Column(name = "total_invested_value_currency", nullable = false, length = 3)
  private String totalInvestedValueCurrency;

  // ─────────────────────────────────────────────────────────────

  @Column(name = "display_currency_code", nullable = false, length = 3)
  private String displayCurrencyCode;

  @Column(name = "has_stale_data", nullable = false)
  private boolean hasStaleData;

  @Column(name = "snapshot_date", nullable = false, updatable = false)
  private Instant snapshotDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  // ─────────────────────────────────────────────────────────────
  // Factory
  // ─────────────────────────────────────────────────────────────

  public static ValuationSnapshotJpaEntity from(ValuationSnapshot domain) {

    ValuationSnapshotJpaEntity e = new ValuationSnapshotJpaEntity();

    e.id = domain.id();

    e.userId = UUID.fromString(domain.userId().toString());

    e.totalValueAmount = domain.totalValue().amount();

    e.totalValueCurrency = domain.totalValue().currency().getCode();

    e.totalCostBasisAmount = domain.totalCostBasis().amount();

    e.totalCostBasisCurrency = domain.totalCostBasis().currency().getCode();

    e.unrealizedGainLossAmount = domain.unrealizedGainLoss().amount();

    e.unrealizedGainLossCurrency = domain.unrealizedGainLoss().currency().getCode();

    e.gainLossPercent = domain.gainLossPercent();

    e.totalCashBalanceAmount = domain.totalCashBalance().amount();

    e.totalCashBalanceCurrency = domain.totalCashBalance().currency().getCode();

    e.totalInvestedValueAmount = domain.totalInvestedValue().amount();

    e.totalInvestedValueCurrency = domain.totalInvestedValue().currency().getCode();

    e.displayCurrencyCode = domain.displayCurrency();

    e.hasStaleData = domain.hasStaleData();

    e.snapshotDate = domain.snapshotDate();

    e.createdAt = Instant.now();

    return e;
  }

  // ─────────────────────────────────────────────────────────────
  // toDomain
  // ─────────────────────────────────────────────────────────────
  public ValuationSnapshot toDomain() {
    return new ValuationSnapshot(id, new UserId(userId),
        Money.of(totalValueAmount, totalValueCurrency),
        Money.of(totalCashBalanceAmount, totalCostBasisCurrency),
        Money.of(unrealizedGainLossAmount, unrealizedGainLossCurrency), gainLossPercent,
        Money.of(totalCashBalanceAmount, totalCashBalanceCurrency),
        Money.of(totalInvestedValueAmount, totalInvestedValueCurrency), displayCurrencyCode,
        hasStaleData, snapshotDate);
  }

}