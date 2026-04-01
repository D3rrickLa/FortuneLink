package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "realized_gains")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED) // for JPA
public class RealizedGainJpaEntity {

  @Id
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private AccountJpaEntity account;

  @Column(name = "symbol", nullable = false, length = 20)
  private String symbol;

  @Column(name = "gain_loss_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal gainLossAmount;

  @Column(name = "gain_loss_currency", nullable = false, length = 3)
  private String gainLossCurrency;

  @Column(name = "cost_basis_sold_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal costBasisSoldAmount;

  @Column(name = "cost_basis_sold_currency", nullable = false, length = 3)
  private String costBasisSoldCurrency;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  public static RealizedGainJpaEntity create(UUID id, AccountJpaEntity account, String symbol,
      BigDecimal gainLossAmount, String gainLossCurrency, BigDecimal costBasisSoldAmount,
      String costBasisSoldCurrency, Instant occurredAt) {

    RealizedGainJpaEntity e = new RealizedGainJpaEntity();
    e.id = id;
    e.account = account;
    e.symbol = symbol;
    e.gainLossAmount = gainLossAmount;
    e.gainLossCurrency = gainLossCurrency;
    e.costBasisSoldAmount = costBasisSoldAmount;
    e.costBasisSoldCurrency = costBasisSoldCurrency;
    e.occurredAt = occurredAt;
    return e;
  }

  void setAccount(AccountJpaEntity account) {
    this.account = account;
  }
}