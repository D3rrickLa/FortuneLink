package com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.FeeJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.TransactionJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bidirectional mapper between the {@code Transaction} domain record and
 * {@code TransactionJpaEntity}.
 * <p>
 * Separate from {@code PortfolioDomainMapper} intentionally. Transactions are not part of the
 * Portfolio aggregate graph — they are loaded independently through {@code TransactionRepository}
 * and have their own lifecycle. Keeping mappers focused on one aggregate boundary prevents them
 * from becoming 800-line god classes.
 * <p>
 * Mapper responsibility boundary:
 * <ul>
 * <li>This class handles Transaction ↔ TransactionJpaEntity and
 * Fee ↔ FeeJpaEntity.</li>
 * <li>It does NOT know about Portfolio or Account domain objects.</li>
 * <li>It does NOT call repositories or services.</li>
 * </ul>
 */
@Component
public class TransactionDomainMapper {

  // =========================================================================
  // toDomain — JPA → Transaction (domain record)
  // =========================================================================

  public Transaction toDomain(TransactionJpaEntity entity) {
    Objects.requireNonNull(entity, "TransactionJpaEntity cannot be null");

    Currency cashCurrency = Currency.of(entity.getCashDeltaCurrency());
    Money cashDelta = new Money(entity.getCashDeltaAmount(), cashCurrency);

    // Reconstruct TradeExecution (present for BUY, SELL, SPLIT, DRIP, ROC)
    TradeExecution execution = null;
    if (entity.getExecutionSymbol() != null) {
      Currency priceCurrency = Currency.of(entity.getExecutionPriceCurrency());
      execution = new TradeExecution(new AssetSymbol(entity.getExecutionSymbol()),
          new Quantity(entity.getExecutionQuantity()),
          new Price(new Money(entity.getExecutionPriceAmount(), priceCurrency)));
    }

    // Reconstruct split Ratio (present only for SPLIT)
    Ratio split = null;
    if (entity.getSplitNumerator() != null && entity.getSplitDenominator() != null) {
      split = new Ratio(entity.getSplitNumerator(), entity.getSplitDenominator());
    }

    // Reconstruct TransactionMetadata
    TransactionMetadata.ExclusionRecord exclusionRecord = null;
    if (entity.isExcluded() && entity.getExcludedAt() != null) {
      exclusionRecord = new TransactionMetadata.ExclusionRecord(entity.getExcludedAt(),
          UserId.fromString(entity.getExcludedBy().toString()), entity.getExcludedReason());
    }

    TransactionMetadata metadata = new TransactionMetadata(AssetType.valueOf(
        entity.getAssetType() != null ? entity.getAssetType() : AssetType.CASH.name()),
        entity.getMetadataSource(), exclusionRecord, entity.getAdditionalData());

    // Reconstruct fees
    List<Fee> fees = entity.getFees().stream().map(this::feeToDomain).toList();

    // Reconstruct related transaction ID
    TransactionId relatedId = entity.getRelatedTransactionId() != null ? TransactionId.fromString(
        entity.getRelatedTransactionId().toString()) : null;

    return Transaction.builder().transactionId(TransactionId.fromString(entity.getId().toString()))
        .accountId(AccountId.fromString(entity.getAccountId().toString()))
        .transactionType(TransactionType.valueOf(entity.getTransactionType())).execution(execution)
        .split(split).cashDelta(cashDelta).fees(fees)
        .notes(entity.getNotes() != null ? entity.getNotes() : "")
        .occurredAt(entity.getOccurredAt()).relatedTransactionId(relatedId).metadata(metadata)
        .build();
  }

  // =========================================================================
  // toEntity — Transaction (domain record) → JPA
  // =========================================================================

  /**
   * Converts a domain {@code Transaction} to a new {@code TransactionJpaEntity}.
   * <p>
   * Always creates a fresh entity. Transactions are immutable in the domain (only exclusion state
   * changes), so there is no update path here — use {@link #applyExclusionState} for exclusion
   * mutations on an existing managed entity.
   *
   * @param domain      the domain transaction record
   * @param portfolioId the owning portfolio UUID (denormalized for query efficiency)
   */
  public TransactionJpaEntity toEntity(Transaction domain, UUID portfolioId) {
    Objects.requireNonNull(domain, "Transaction cannot be null");
    Objects.requireNonNull(portfolioId, "portfolioId cannot be null");

    // TradeExecution fields (all null for non-trade types)
    String execSymbol = null;
    java.math.BigDecimal execQty = null;
    java.math.BigDecimal execPrice = null;
    String execPriceCurrency = null;

    if (domain.execution() != null) {
      execSymbol = domain.execution().asset().symbol();
      execQty = domain.execution().quantity().amount();
      execPrice = domain.execution().pricePerUnit().amount();
      execPriceCurrency = domain.execution().pricePerUnit().currency().getCode();
    }

    // Split ratio (null for non-SPLIT)
    Integer splitNum = domain.split() != null ? domain.split().numerator() : null;
    Integer splitDenom = domain.split() != null ? domain.split().denominator() : null;

    // Exclusion
    TransactionMetadata.ExclusionRecord excl = domain.metadata().exclusion();
    UUID excludedBy = null;
    Instant excludedAt = null;
    String excludedReason = null;
    if (excl != null) {
      excludedBy = UUID.fromString(excl.by().toString());
      excludedAt = excl.occurredAt();
      excludedReason = excl.reason();
    }

    UUID relatedId = domain.relatedTransactionId() != null ? UUID.fromString(
        domain.relatedTransactionId().toString()) : null;

    // Asset type: prefer execution type, fall back to metadata
    String assetTypeName =
        domain.metadata().assetType() != null ? domain.metadata().assetType().name() : null;

    TransactionJpaEntity entity = TransactionJpaEntity.create(
        UUID.fromString(domain.transactionId().toString()), portfolioId,
        UUID.fromString(domain.accountId().toString()), domain.transactionType().name(), execSymbol,
        execQty, execPrice, execPriceCurrency, assetTypeName, splitNum, splitDenom,
        domain.cashDelta().amount(), domain.cashDelta().currency().getCode(),
        domain.metadata().source(), domain.isExcluded(), excludedAt, excludedBy, excludedReason,
        domain.metadata().additionalData(), domain.notes(), domain.occurredAt(), relatedId);

    // Map fees
    List<FeeJpaEntity> feeEntities = domain.fees().stream()
        .map(f -> feeToEntity(UUID.randomUUID(), entity, f)).toList();
    entity.replaceFees(feeEntities);

    return entity;
  }

  /**
   * Applies only the exclusion mutation to a managed JPA entity. Call this instead of
   * {@code toEntity} when restoring or excluding an existing transaction — avoids a pointless
   * re-insert of all fee rows.
   */
  public void applyExclusionState(Transaction domain, TransactionJpaEntity managed) {
    Objects.requireNonNull(domain, "Transaction cannot be null");
    Objects.requireNonNull(managed, "Managed entity cannot be null");

    TransactionMetadata.ExclusionRecord excl = domain.metadata().exclusion();
    if (excl != null) {
      managed.applyExclusionState(true, excl.occurredAt(), UUID.fromString(excl.by().toString()),
          excl.reason());
    } else {
      managed.applyExclusionState(false, null, null, null);
    }
  }

  // =========================================================================
  // Fee helpers
  // =========================================================================

  private Fee feeToDomain(FeeJpaEntity fe) {
    Currency nativeCurrency = Currency.of(fe.getNativeCurrency());
    Money nativeAmount = new Money(fe.getNativeAmount(), nativeCurrency);

    Money accountAmount = null;
    if (fe.getAccountAmount() != null && fe.getAccountAmountCurrency() != null) {
      accountAmount = new Money(fe.getAccountAmount(), Currency.of(fe.getAccountAmountCurrency()));
    }

    ExchangeRate exchangeRate = null;
    if (fe.getExchangeRate() != null && fe.getFromCurrency() != null && fe.getToCurrency() != null
        && fe.getExchangeRateDate() != null) {
      exchangeRate = new ExchangeRate(Currency.of(fe.getFromCurrency()),
          Currency.of(fe.getToCurrency()), fe.getExchangeRate(), fe.getExchangeRateDate());
    }

    return new Fee(FeeType.valueOf(fe.getFeeType()), nativeAmount, accountAmount, exchangeRate,
        fe.getOccurredAt(), new Fee.FeeMetadata(Map.of()) // metadata not persisted — by design
    );
  }

  private FeeJpaEntity feeToEntity(UUID id, TransactionJpaEntity transactionEntity, Fee fee) {
    java.math.BigDecimal accountAmt =
        fee.accountAmount() != null ? fee.accountAmount().amount() : null;
    String accountAmtCurrency =
        fee.accountAmount() != null ? fee.accountAmount().currency().getCode() : null;
    java.math.BigDecimal rate = fee.exchangeRate() != null ? fee.exchangeRate().rate() : null;
    String fromCcy = fee.exchangeRate() != null ? fee.exchangeRate().from().getCode() : null;
    String toCcy = fee.exchangeRate() != null ? fee.exchangeRate().to().getCode() : null;
    Instant rateDate = fee.exchangeRate() != null ? fee.exchangeRate().quotedAt() : null;

    return FeeJpaEntity.create(id, transactionEntity, fee.feeType().name(),
        fee.nativeAmount().amount(), fee.nativeAmount().currency().getCode(), accountAmt,
        accountAmtCurrency, rate, fromCcy, toCcy, rateDate, fee.occurredAt());
  }
}