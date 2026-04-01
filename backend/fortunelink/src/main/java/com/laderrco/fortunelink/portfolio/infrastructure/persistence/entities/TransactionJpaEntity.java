package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import com.laderrco.fortunelink.portfolio.infrastructure.persistence.converters.StringMapConverter;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Persistence model for the {@code Transaction} domain record.
 * <p>
 * Column mapping rationale:
 * <ul>
 * <li><b>TradeExecution</b> — flattened into nullable columns. The execution is
 * optional (null for DEPOSIT, WITHDRAWAL, etc.) so an @Embeddable would
 * require {@code @Column(nullable=true)} on all fields anyway. Flattening
 * is cleaner.</li>
 * <li><b>TransactionMetadata</b> — assetType and source are columns; exclusion
 * fields are columns (added V3); additionalData is JSONB because it is a
 * free-form Map used for audit/vendor-specific keys.</li>
 * <li><b>Fees</b> — @OneToMany to {@code transaction_fees} (V1 schema). This
 * preserves the existing table structure and allows individual fee
 * queries.</li>
 * <li><b>cashDelta</b> — stored as two columns (amount + currency). Required
 * for
 * the full-account replay path in {@code replayFullTransaction}.</li>
 * </ul>
 *
 * <p>
 * <b>What is NOT mapped here:</b>
 * {@code MarketAssetQuote} is never persisted — it is ephemeral market data
 * retrieved from Redis/API on demand. Do not add JPA mapping for it.
 */
@Entity
@Getter
@Table(name = "transactions")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED) // for JPA
public class TransactionJpaEntity {

  @Id
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  // The V1 schema has portfolio_id for join queries — store it denormalized.
  @Column(name = "portfolio_id", nullable = false, columnDefinition = "uuid")
  private UUID portfolioId;

  @Column(name = "account_id", nullable = false, columnDefinition = "uuid")
  private UUID accountId;

  @Column(name = "transaction_type", nullable = false, length = 50)
  private String transactionType;

  // -------------------------------------------------------------------------
  // TradeExecution (flattened, all nullable — absent for non-trade types)
  // -------------------------------------------------------------------------

  /** AssetSymbol.symbol() — null for DEPOSIT, WITHDRAWAL, FEE, etc. */
  @Column(name = "execution_symbol", length = 100)
  private String executionSymbol;

  @Column(name = "execution_quantity", precision = 20, scale = 8)
  private BigDecimal executionQuantity;

  @Column(name = "execution_price_amount", precision = 20, scale = 10)
  private BigDecimal executionPriceAmount;

  @Column(name = "execution_price_currency", length = 3)
  private String executionPriceCurrency;

  /**
   * AssetType from TradeExecution / TransactionMetadata.
   * Stored as the V1 {@code asset_type} column.
   */
  @Column(name = "asset_type", length = 50)
  private String assetType;

  // Split ratio (null unless transactionType = SPLIT)
  @Column(name = "split_numerator")
  private Integer splitNumerator;

  @Column(name = "split_denominator")
  private Integer splitDenominator;

  @Column(name = "cash_delta_amount", precision = 20, scale = 10, nullable = false)
  private BigDecimal cashDeltaAmount;

  @Column(name = "cash_delta_currency", length = 3, nullable = false)
  private String cashDeltaCurrency;

  @Column(name = "metadata_source", nullable = false, length = 50)
  private String metadataSource;

  @Column(name = "excluded", nullable = false)
  private boolean excluded;

  @Column(name = "excluded_at")
  private Instant excludedAt;

  @Column(name = "excluded_by", columnDefinition = "uuid")
  private UUID excludedBy;

  @Column(name = "excluded_reason", columnDefinition = "text")
  private String excludedReason;

  /**
   * TransactionMetadata.additionalData — free-form key/value pairs.
   * Stored as JSONB. The V1 {@code metadata} column already exists for this.
   * {@code StringMapConverter} serialises Map&lt;String,String&gt; ↔ JSON text.
   */
  @Convert(converter = StringMapConverter.class)
  @Column(name = "additional_data", columnDefinition = "jsonb")
  private Map<String, String> additionalData = new HashMap<>();

  @Column(name = "notes", columnDefinition = "text")
  private String notes;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Self-referential link for reversals / paired trades. */
  @Column(name = "related_transaction_id", columnDefinition = "uuid")
  private UUID relatedTransactionId;

  @Version
  @Column(name = "version", nullable = false)
  private int version;

  // fees are always needed with the transaction
  @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<FeeJpaEntity> fees = new ArrayList<>();

  public static TransactionJpaEntity create(UUID id, UUID portfolioId, UUID accountId,
      String transactionType, String executionSymbol, BigDecimal executionQuantity,
      BigDecimal executionPriceAmount, String executionPriceCurrency, String assetType,
      Integer splitNumerator, Integer splitDenominator, BigDecimal cashDeltaAmount,
      String cashDeltaCurrency, String metadataSource, boolean excluded, Instant excludedAt,
      UUID excludedBy, String excludedReason, Map<String, String> additionalData,
      String notes, Instant occurredAt, UUID relatedTransactionId) {

    TransactionJpaEntity e = new TransactionJpaEntity();
    e.id = id;
    e.portfolioId = portfolioId;
    e.accountId = accountId;
    e.transactionType = transactionType;
    e.executionSymbol = executionSymbol;
    e.executionQuantity = executionQuantity;
    e.executionPriceAmount = executionPriceAmount;
    e.executionPriceCurrency = executionPriceCurrency;
    e.assetType = assetType;
    e.splitNumerator = splitNumerator;
    e.splitDenominator = splitDenominator;
    e.cashDeltaAmount = cashDeltaAmount;
    e.cashDeltaCurrency = cashDeltaCurrency;
    e.metadataSource = metadataSource;
    e.excluded = excluded;
    e.excludedAt = excludedAt;
    e.excludedBy = excludedBy;
    e.excludedReason = excludedReason;
    e.additionalData = additionalData != null ? new HashMap<>(additionalData) : new HashMap<>();
    e.notes = notes;
    e.occurredAt = occurredAt;
    e.relatedTransactionId = relatedTransactionId;
    e.createdAt = Instant.now();
    return e;
  }

  // -------------------------------------------------------------------------
  // Mutation (exclusion lifecycle only — transactions are otherwise immutable)
  // -------------------------------------------------------------------------

  /**
   * Applies exclusion state from an updated domain record.
   * Transactions are immutable in the domain — only exclusion metadata changes
   * post-creation, so this is the only mutable operation on this entity.
   */
  public void applyExclusionState(boolean excluded, Instant excludedAt,
      UUID excludedBy, String excludedReason) {
    this.excluded = excluded;
    this.excludedAt = excludedAt;
    this.excludedBy = excludedBy;
    this.excludedReason = excludedReason;
  }

  public void replaceFees(List<FeeJpaEntity> incoming) {
    this.fees.clear();
    for (FeeJpaEntity f : incoming) {
      f.setTransaction(this);
      this.fees.add(f);
    }
  }

  public Map<String, String> getAdditionalData() {
    return Collections.unmodifiableMap(additionalData);
  }

  public List<FeeJpaEntity> getFees() {
    return Collections.unmodifiableList(fees);
  }
}