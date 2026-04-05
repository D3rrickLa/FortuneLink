package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * Persistence model for {@code Account}.
 * <p>
 * Owns a bi-directional relationship to {@code PortfolioJpaEntity} and
 * one-directional collections
 * to {@code PositionJpaEntity}, {@code TransactionJpaEntity}, and
 * {@code RealizedGainJpaEntity}.
 */
@Entity
@Getter
@Table(name = "accounts")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AccountJpaEntity implements Persistable<UUID> {

  @Id
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "portfolio_id", nullable = false)
  private PortfolioJpaEntity portfolio;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "account_type", nullable = false, length = 50)
  private String accountType;

  @Column(name = "base_currency_code", nullable = false, length = 3)
  private String baseCurrencyCode;

  @Column(name = "position_strategy", nullable = false, length = 30)
  private String positionStrategy;

  @Column(name = "health_status", nullable = false, length = 20)
  private String healthStatus;

  @Column(name = "lifecycle_state", nullable = false, length = 20)
  private String lifecycleState;

  @Column(name = "cash_balance_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal cashBalanceAmount;

  @Column(name = "cash_balance_currency", nullable = false, length = 3)
  private String cashBalanceCurrency;

  @Column(name = "closed_date")
  private Instant closedDate;

  @Column(name = "created_date", nullable = false, updatable = false)
  private Instant createdDate;

  @Column(name = "last_updated_on", nullable = false)
  private Instant lastUpdatedOn;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @Transient
  private boolean isNew = true;

  // -------------------------------------------------------------------------
  // Relationships
  // -------------------------------------------------------------------------

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private final Set<PositionJpaEntity> positions = new LinkedHashSet<>();

  // if a single portoflio lods like 3 years of active trading, that's 100+
  // records, each time they open the portfolio page, each one is 'loaded', LAZY
  // to solve this
  @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST,
      CascadeType.MERGE }, orphanRemoval = false, fetch = FetchType.LAZY)
  private final Set<RealizedGainJpaEntity> realizedGains = new LinkedHashSet<>();

  // -------------------------------------------------------------------------
  // Factory
  // -------------------------------------------------------------------------

  public static AccountJpaEntity create(UUID id, PortfolioJpaEntity portfolio, String name,
      String accountType, String baseCurrencyCode, String positionStrategy, String healthStatus,
      String lifecycleState, BigDecimal cashBalanceAmount, String cashBalanceCurrency,
      Instant closedDate, Instant createdDate, Instant lastUpdatedOn) {

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
    e.closedDate = closedDate;
    e.createdDate = createdDate;
    e.lastUpdatedOn = lastUpdatedOn;
    return e;
  }

  // -------------------------------------------------------------------------
  // In-place update
  // -------------------------------------------------------------------------

  public void applyFrom(AccountJpaEntity source) {
    this.name = source.name;
    this.accountType = source.accountType;
    this.positionStrategy = source.positionStrategy;
    this.healthStatus = source.healthStatus;
    this.lifecycleState = source.lifecycleState;
    this.cashBalanceAmount = source.cashBalanceAmount;
    this.cashBalanceCurrency = source.cashBalanceCurrency;
    this.closedDate = source.closedDate;
    this.lastUpdatedOn = source.lastUpdatedOn;
    replacePositions(source.positions);
    // NOTE: realized gains are NOT replaced here, use addNewRealizedGains instead.
    // Calling replacePositions is safe because positions are fully rebuilt by
    // PositionRecalculationService. Gains are append-only and must never be
    // cleared.
  }

  public void replacePositions(List<PositionJpaEntity> incoming) {
    Map<UUID, PositionJpaEntity> existing = new HashMap<>();
    for (PositionJpaEntity p : this.positions) {
      existing.put(p.getId(), p);
    }

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

  /**
   * Appends only NEW realized gain rows, those whose UUID does not already exist
   * in the persisted
   * collection. This is the correct operation for append-only domain data. Never
   * call clear() on
   * realizedGains.
   *
   * <p>
   * The mapper is responsible for diffing domain IDs vs persisted IDs and passing
   * only the delta
   * here.
   */
  public void addNewRealizedGains(List<RealizedGainJpaEntity> newGains) {
    for (RealizedGainJpaEntity g : newGains) {
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

  void setPortfolio(PortfolioJpaEntity portfolio) {
    this.portfolio = portfolio;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }
}