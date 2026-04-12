package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps the {@code transaction_fees} table.
 * <p>
 * The domain {@code Fee} record supports multi-currency: it carries both a {@code nativeAmount}
 * (original currency) and an {@code accountAmount} (converted to account currency) plus the
 * {@code ExchangeRate} used. All three are persisted here so they can be reconstructed exactly.
 * <p>
 * {@code FeeMetadata} key/value pairs are NOT persisted separately , they are rarely needed after
 * recording and can be reconstructed from the {@code additionalData} JSONB on
 * {@code TransactionJpaEntity} if required.
 */
@Entity
@Data
@Table(name = "transaction_fees")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED) // for JPA
public class FeeJpaEntity {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "transaction_id", nullable = false)
  private TransactionJpaEntity transaction;
  @Column(name = "fee_type", length = 50)
  private String feeType; // FeeType enum name
  // Native amount , the currency the fee was charged in
  @Column(name = "native_amount", nullable = false, precision = 20, scale = 10)
  private BigDecimal nativeAmount;
  @Column(name = "native_currency", nullable = false, length = 3)
  private String nativeCurrency;
  // Account amount , post-conversion (null if same currency as account)
  @Column(name = "account_amount", precision = 20, scale = 10)
  private BigDecimal accountAmount;
  @Column(name = "account_amount_currency", length = 3)
  private String accountAmountCurrency;
  // Exchange rate components (null if no conversion was needed)
  @Column(name = "exchange_rate", precision = 20, scale = 10)
  private BigDecimal exchangeRate;
  @Column(name = "rate_from_currency", length = 3)
  private String fromCurrency;
  @Column(name = "rate_to_currency", length = 3)
  private String toCurrency;
  @Column(name = "exchange_rate_date")
  private Instant exchangeRateDate;
  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;
  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  public static FeeJpaEntity createEmpty() {
    return new FeeJpaEntity();
  }

  public static FeeJpaEntity create(UUID id, TransactionJpaEntity transaction, String feeType,
      BigDecimal nativeAmount, String nativeCurrency, BigDecimal accountAmount,
      String accountAmountCurrency, BigDecimal exchangeRate, String fromCurrency, String toCurrency,
      Instant exchangeRateDate, Instant occurredAt) {

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

  public void setTransaction(TransactionJpaEntity transaction) {
    this.transaction = transaction;
  }
}