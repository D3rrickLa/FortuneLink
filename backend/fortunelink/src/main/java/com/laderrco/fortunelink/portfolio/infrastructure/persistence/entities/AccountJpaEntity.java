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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * Persistence model for {@code Account}.
 * <p>
 * Owns a bidirectional relationship to {@code PortfolioJpaEntity} and one-directional collections
 * to {@code PositionJpaEntity}, {@code TransactionJpaEntity}, and {@code RealizedGainJpaEntity}.
 */
@Entity
@Getter
@Table(name = "accounts")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AccountJpaEntity implements Persistable<UUID> {

  @OneToMany(mappedBy = "account", cascade = {CascadeType.MERGE,
      CascadeType.PERSIST}, orphanRemoval = true, fetch = FetchType.LAZY)
  private final Set<PositionJpaEntity> positions = new LinkedHashSet<>();
  // if a single portfolio loads 3 years of active trading, that's 100+
  // records, each time they open the portfolio page, each one is 'loaded', LAZY
  // to solve this
  @OneToMany(mappedBy = "account", cascade = {CascadeType.PERSIST,
      CascadeType.MERGE}, orphanRemoval = false, fetch = FetchType.LAZY)
  private final Set<RealizedGainJpaEntity> realizedGains = new LinkedHashSet<>();
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

  // -------------------------------------------------------------------------
  // Relationships
  // -------------------------------------------------------------------------
  @Version
  @Column(name = "version", nullable = false)
  private Long version;
  @Transient
  private boolean isNew = true;

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
    // Positions are managed exclusively by PortfolioDomainMapper.accountToEntity()
    // after this method returns. Calling replacePositions() here clears the
    // managed collection before the mapper can diff it, which causes INSERT
    // attempts on already-persisted symbols. Same reasoning as realized gains.
    // replacePositions(source.positions); <-- REMOVED
  }

  /**
   * Updates only scalar columns. Positions and realized gains are intentionally excluded — they
   * have their own diff/append logic in PortfolioDomainMapper. Calling this is safe at any point in
   * the mapping cycle because it never clears any collection.
   */
  public void applyScalarFields(String name, String accountType, String positionStrategy,
      String healthStatus, String lifecycleState, BigDecimal cashBalanceAmount,
      String cashBalanceCurrency, Instant closedDate, Instant lastUpdatedOn) {
    this.name = name;
    this.accountType = accountType;
    this.positionStrategy = positionStrategy;
    this.healthStatus = healthStatus;
    this.lifecycleState = lifecycleState;
    this.cashBalanceAmount = cashBalanceAmount;
    this.cashBalanceCurrency = cashBalanceCurrency;
    this.closedDate = closedDate;
    this.lastUpdatedOn = lastUpdatedOn;
  }

  public void replacePositions(Set<PositionJpaEntity> incoming) {
    Map<String, PositionJpaEntity> existingBySymbol = new HashMap<>();

    for (PositionJpaEntity p : this.positions) {
      existingBySymbol.put(p.getSymbol(), p);
    }

    this.positions.clear();

    for (PositionJpaEntity incomingPos : incoming) {

      PositionJpaEntity existing = existingBySymbol.get(incomingPos.getSymbol());

      if (existing != null) {
        // UPDATE managed entity instead of inserting new one
        existing.applyFrom(incomingPos);
        this.positions.add(existing);

      } else {
        incomingPos.setAccount(this);
        this.positions.add(incomingPos);
      }
    }
  }

  /**
   * Appends only NEW realized gain rows, those whose UUID does not already exist in the persisted
   * collection. This is the correct operation for append-only domain data. Never call clear() on
   * realizedGains.
   *
   * <p>
   * The mapper is responsible for diffing domain IDs vs persisted IDs and passing only the delta
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

  public Set<PositionJpaEntity> getPositions() {
    return Collections.unmodifiableSet(positions);
  }

  public Set<RealizedGainJpaEntity> getRealizedGains() {
    return Collections.unmodifiableSet(realizedGains);
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