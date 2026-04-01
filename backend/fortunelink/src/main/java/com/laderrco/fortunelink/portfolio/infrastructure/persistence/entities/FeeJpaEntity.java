package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Maps the {@code transaction_fees} table.
 * <p>
 * The domain {@code Fee} record supports multi-currency: it carries both a
 * {@code nativeAmount} (original currency) and an {@code accountAmount}
 * (converted to account currency) plus the {@code ExchangeRate} used.
 * All three are persisted here so they can be reconstructed exactly.
 * <p>
 * {@code FeeMetadata} key/value pairs are NOT persisted separately — they
 * are rarely needed after recording and can be reconstructed from the
 * {@code additionalData} JSONB on {@code TransactionJpaEntity} if required.
 */
@Entity
@Getter
@Table(name = "transaction_fees")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED) // for JPA
public class FeeJpaEntity {

  @Id
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transaction_id", nullable = false)
  private TransactionJpaEntity transaction;

  @Column(name = "fee_type", length = 50)
  private String feeType; // FeeType enum name

  // Native amount — the currency the fee was charged in
  @Column(name = "fee_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal nativeAmount;

  @Column(name = "fee_currency", nullable = false, length = 3)
  private String nativeCurrency;

  // Account amount — post-conversion (null if same currency as account)
  @Column(name = "account_amount", precision = 20, scale = 10)
  private BigDecimal accountAmount;

  @Column(name = "account_amount_currency", length = 3)
  private String accountAmountCurrency;

  // Exchange rate components (null if no conversion was needed)
  @Column(name = "rate", precision = 20, scale = 10)
  private BigDecimal exchangeRate;

  @Column(name = "from_currency", length = 3)
  private String fromCurrency;

  @Column(name = "to_currency", length = 3)
  private String toCurrency;

  @Column(name = "exchange_rate_date")
  private Instant exchangeRateDate;

  @Column(name = "fee_date", nullable = false)
  private Instant occurredAt;

  @Version
  @Column(name = "version", nullable = false)
  private int version;

  public static FeeJpaEntity create(UUID id, TransactionJpaEntity transaction, String feeType,
      BigDecimal nativeAmount, String nativeCurrency, BigDecimal accountAmount,
      String accountAmountCurrency, BigDecimal exchangeRate, String fromCurrency,
      String toCurrency, Instant exchangeRateDate, Instant occurredAt) {

    FeeJpaEntity e = new FeeJpaEntity();
    e.id = id;
    e.transaction = transaction;
    e.feeType = feeType;
    e.nativeAmount = nativeAmount;
    e.nativeCurrency = nativeCurrency;
    e.accountAmount = accountAmount;
    e.accountAmountCurrency = accountAmountCurrency;
    e.exchangeRate = exchangeRate;
    e.fromCurrency = fromCurrency;
    e.toCurrency = toCurrency;
    e.exchangeRateDate = exchangeRateDate;
    e.occurredAt = occurredAt;
    return e;
  }

  void setTransaction(TransactionJpaEntity transaction) {
    this.transaction = transaction;
  }
}