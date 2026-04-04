package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Maps the {@code assets} table to domain {@code AcbPosition} (MVP) or {@code FifoPosition}
 * (future) via the mapper.
 * <p>
 * For the ACB MVP, each row represents the aggregate state of one symbol inside one account: total
 * quantity, total cost basis, and acquisition date.
 * <p>
 * FIFO with tax lots would require a separate {@code tax_lots} table. The {@code identifier_type}
 * discriminator column is kept to stay compatible with the V1 schema and support future
 * polymorphism.
 */
@Entity
@Getter
@Table(name = "positions")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED) // for JPA
public class PositionJpaEntity {

  @Id
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private AccountJpaEntity account;

  /**
   * Discriminator: 'MARKET', 'CRYPTO', or 'CASH'. Derived from {@code AssetType} during mapping.
   */
  @Column(name = "identifier_type", nullable = false, length = 50)
  private String identifierType;

  /**
   * Ticker or currency code, maps to {@code AssetSymbol.symbol()}.
   */
  @Column(name = "symbol", nullable = false, length = 20)
  private String symbol;

  @Column(name = "asset_type", length = 50)
  private String assetType; // AssetType enum name

  @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
  private BigDecimal quantity; // AcbPosition.totalQuantity

  @Column(name = "cost_basis_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal costBasisAmount; // AcbPosition.totalCostBasis.amount

  @Column(name = "cost_basis_currency", nullable = false, length = 3)
  private String costBasisCurrency; // AcbPosition.totalCostBasis.currency

  @Column(name = "first_acquired_at", nullable = false)
  private Instant acquiredDate; // AcbPosition.firstAcquiredAt

  @Column(name = "last_modified_at")
  private Instant lastModifiedAt; // AcbPosition.lastModifiedAt

  @Version
  @Column(name = "version", nullable = false)
  private int version;

  // -------------------------------------------------------------------------
  // Factory
  // -------------------------------------------------------------------------

  public static PositionJpaEntity create(UUID id, AccountJpaEntity account, String identifierType,
      String symbol, String assetType, BigDecimal quantity, BigDecimal costBasisAmount,
      String costBasisCurrency, Instant acquiredDate, Instant lastModifiedAt) {

    PositionJpaEntity e = new PositionJpaEntity();
    e.id = id;
    e.account = account;
    e.identifierType = identifierType;
    e.symbol = symbol;
    e.assetType = assetType;
    e.quantity = quantity;
    e.costBasisAmount = costBasisAmount;
    e.costBasisCurrency = costBasisCurrency;
    e.acquiredDate = acquiredDate;
    e.lastModifiedAt = lastModifiedAt;
    return e;
  }

  public void applyFrom(PositionJpaEntity source) {
    this.identifierType = source.identifierType;
    this.assetType = source.assetType;
    this.quantity = source.quantity;
    this.costBasisAmount = source.costBasisAmount;
    this.costBasisCurrency = source.costBasisCurrency;
    this.acquiredDate = source.acquiredDate;
    this.lastModifiedAt = source.lastModifiedAt;
  }

  void setAccount(AccountJpaEntity account) {
    this.account = account;
  }
}