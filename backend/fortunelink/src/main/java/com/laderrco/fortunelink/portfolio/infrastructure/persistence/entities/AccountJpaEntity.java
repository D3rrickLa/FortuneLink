package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import java.time.Instant;
import java.util.*;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import jakarta.persistence.*;
import lombok.Getter;

/**
 * Persistence model for {@code Account}.
 * <p>
 * Owns a bi-directional relationship to {@code PortfolioJpaEntity} and
 * one-directional collections to {@code PositionJpaEntity},
 * {@code TransactionJpaEntity}, and {@code RealizedGainJpaEntity}.
 */
@Entity
@Getter
@Table(name = "accounts")
public class AccountJpaEntity {

  @Id
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "portfolio_id", nullable = false)
  private PortfolioJpaEntity portfolio;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "account_type", nullable = false, length = 50)
  private String accountType; // AccountType enum name

  @Column(name = "base_currency", nullable = false, length = 3)
  private String baseCurrencyCode;

  @Column(name = "position_strategy", nullable = false, length = 30)
  private String positionStrategy; // PositionStrategy enum name — added in V3

  @Column(name = "health_status", nullable = false, length = 20)
  private String healthStatus; // HealthStatus enum name — added in V3

  @Column(name = "lifecycle_state", nullable = false, length = 20)
  private String lifecycleState; // AccountLifecycleState enum name — added in V3

  // Money fields inlined — amounts stored separately per DB column.
  // We do NOT use @Embedded here because the column names differ from
  // MoneyEmbeddable defaults and having explicit columns is clearer.
  @Column(name = "cash_balance_amount", nullable = false, precision = 20, scale = 10)
  private java.math.BigDecimal cashBalanceAmount;

  @Column(name = "cash_balance_currency", nullable = false, length = 3)
  private String cashBalanceCurrency;

  @Column(name = "is_active", nullable = false)
  private boolean active; // legacy column; kept for queries that still use it

  @Column(name = "closed_date")
  private Instant closedDate;

  @Column(name = "created_date", nullable = false, updatable = false)
  private Instant createdDate;

  @Column(name = "last_system_interaction", nullable = false)
  private Instant lastUpdatedOn;

  @Version
  @Column(name = "version", nullable = false)
  private int version;

  // -------------------------------------------------------------------------
  // Relationships
  // -------------------------------------------------------------------------

  /**
   * Current open positions. Loaded eagerly because the domain always
   * reconstructs the full PositionBook when an Account is loaded.
   * If this becomes a performance issue, profile first — lazy-loading
   * positions causes N+1 on every portfolio read anyway.
   */
  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<PositionJpaEntity> positions = new ArrayList<>();

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<RealizedGainJpaEntity> realizedGains = new ArrayList<>();

  // Transactions are NOT eagerly loaded — they're fetched separately via
  // TransactionRepository to avoid pulling the entire history on every load.

  // -------------------------------------------------------------------------
  // JPA
  // -------------------------------------------------------------------------

  protected AccountJpaEntity() {
  }

  // -------------------------------------------------------------------------
  // Factory
  // -------------------------------------------------------------------------

  public static AccountJpaEntity create(
      UUID id,
      PortfolioJpaEntity portfolio,
      String name,
      String accountType,
      String baseCurrencyCode,
      String positionStrategy,
      String healthStatus,
      String lifecycleState,
      java.math.BigDecimal cashBalanceAmount,
      String cashBalanceCurrency,
      boolean active,
      Instant closedDate,
      Instant createdDate,
      Instant lastUpdatedOn) {

    AccountJpaEntity e = new AccountJpaEntity();
    e.id = id;
    e.portfolio = portfolio;
    e.name = name;
    e.accountType = accountType;
    e.baseCurrencyCode = baseCurrencyCode;
    e.positionStrategy = positionStrategy;
    e.healthStatus = healthStatus;
    e.lifecycleState = lifecycleState;
    e.cashBalanceAmount = cashBalanceAmount;
    e.cashBalanceCurrency = cashBalanceCurrency;
    e.active = active;
    e.closedDate = closedDate;
    e.createdDate = createdDate;
    e.lastUpdatedOn = lastUpdatedOn;
    return e;
  }

  // -------------------------------------------------------------------------
  // In-place update — called by PortfolioJpaEntity.replaceAccounts()
  // -------------------------------------------------------------------------

  public void applyFrom(AccountJpaEntity source) {
    this.name = source.name;
    this.accountType = source.accountType;
    this.positionStrategy = source.positionStrategy;
    this.healthStatus = source.healthStatus;
    this.lifecycleState = source.lifecycleState;
    this.cashBalanceAmount = source.cashBalanceAmount;
    this.cashBalanceCurrency = source.cashBalanceCurrency;
    this.active = source.active;
    this.closedDate = source.closedDate;
    this.lastUpdatedOn = source.lastUpdatedOn;
    replacePositions(source.positions);
    replaceRealizedGains(source.realizedGains);
  }

  public void replacePositions(List<PositionJpaEntity> incoming) {
    Map<UUID, PositionJpaEntity> existing = new HashMap<>();
    for (PositionJpaEntity p : this.positions)
      existing.put(p.getId(), p);

    this.positions.clear();
    for (PositionJpaEntity p : incoming) {
      PositionJpaEntity cur = existing.get(p.getId());
      if (cur != null) {
        cur.applyFrom(p);
        this.positions.add(cur);
      } else {
        p.setAccount(this);
        this.positions.add(p);
      }
    }
  }

  public void replaceRealizedGains(List<RealizedGainJpaEntity> incoming) {
    // Realized gains are append-only; the account mapper reconstructs
    // the full list from domain state on every save. Clear and re-add.
    this.realizedGains.clear();
    for (RealizedGainJpaEntity g : incoming) {
      g.setAccount(this);
      this.realizedGains.add(g);
    }
  }

  // -------------------------------------------------------------------------
  // Getters
  // -------------------------------------------------------------------------

  public List<PositionJpaEntity> getPositions() {
    return Collections.unmodifiableList(positions);
  }

  public List<RealizedGainJpaEntity> getRealizedGains() {
    return Collections.unmodifiableList(realizedGains);
  }

  // Package-private — only PortfolioJpaEntity.replaceAccounts() needs this
  void setPortfolio(PortfolioJpaEntity portfolio) {
    this.portfolio = portfolio;
  }
}