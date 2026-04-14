package com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction.TradeExecution;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.FeeJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.TransactionJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TransactionDomainMapper Unit Tests")
class TransactionDomainMapperTest {

  private static final UUID TX_UUID = UUID.randomUUID();
  private static final UUID PORTFOLIO_UUID = UUID.randomUUID();
  private static final UUID ACCOUNT_UUID = UUID.randomUUID();
  private static final UUID USER_UUID = UUID.randomUUID();
  private static final String IDEM_KEY = "idem-123";
  private TransactionDomainMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new TransactionDomainMapper();
  }

  private TransactionJpaEntity createBaseJpaEntity() {
    return TransactionJpaEntity.create(TX_UUID, PORTFOLIO_UUID, ACCOUNT_UUID, "BUY", null, null,
        null, null, "STOCK", null, null, BigDecimal.ZERO, "USD", "MANUAL", false, null, null, null,
        null, "Notes", Instant.now(), null, IDEM_KEY);
  }

  private Transaction createDomainBuyTransaction() {
    TradeExecution exec = new TradeExecution(new AssetSymbol("MSFT"), new Quantity(BigDecimal.ONE),
        new Price(new Money(new BigDecimal("300.00"), Currency.of("USD"))));

    Fee fee = new Fee(FeeType.COMMISSION, new Money(new BigDecimal("2.50"), Currency.of("USD")),
        null, null, Instant.now(), new Fee.FeeMetadata(java.util.Map.of()));

    return Transaction.builder().transactionId(TransactionId.fromString(TX_UUID.toString()))
        .accountId(AccountId.fromString(ACCOUNT_UUID.toString()))
        .transactionType(TransactionType.BUY).execution(exec).fees(List.of(fee))
        .cashDelta(new Money(new BigDecimal("-302.50"), Currency.of("USD")))
        .metadata(new TransactionMetadata(AssetType.STOCK, "MANUAL", null, null))
        .occurredAt(Instant.now()).notes("NOTES").build();
  }

  @Nested
  @DisplayName("toDomain - JPA to Transaction")
  class ToDomainMapping {

    @Test
    @DisplayName("should map full BUY transaction with fees and exclusion")
    void shouldMapFullBuyTransaction() {

      TransactionJpaEntity entity = createBaseJpaEntity();
      entity.setExecutionSymbol("AAPL");
      entity.setExecutionQuantity(new BigDecimal("10"));
      entity.setExecutionPriceAmount(new BigDecimal("150.00"));
      entity.setExecutionPriceCurrency("USD");
      entity.setExcluded(true);
      entity.setExcludedAt(Instant.now());
      entity.setExcludedBy(USER_UUID);
      entity.setCashDeltaAmount(BigDecimal.valueOf(-1505));
      entity.setCashDeltaCurrency("USD");
      entity.setExcludedReason("Duplicate");

      FeeJpaEntity fee = FeeJpaEntity.create(entity, "COMMISSION",
          new BigDecimal("5.00"), "USD", null, null, null, null, null, null, Instant.now());
      entity.replaceFees(List.of(fee));

      Transaction domain = mapper.toDomain(entity);

      assertThat(domain.transactionType()).isEqualTo(TransactionType.BUY);
      assertThat(domain.execution().asset().symbol()).isEqualTo("AAPL");
      assertThat(domain.metadata().exclusion()).isNotNull();
      assertThat(domain.metadata().exclusion().reason()).isEqualTo("Duplicate");
      assertThat(domain.fees()).hasSize(1);
      assertThat(domain.fees().get(0).feeType()).isEqualTo(FeeType.COMMISSION);
    }

    @Test
    @DisplayName("should map SPLIT transaction with ratio")
    void shouldMapSplitTransaction() {

      TransactionJpaEntity entity = createBaseJpaEntity();
      entity.setTransactionType("SPLIT");
      entity.setSplitNumerator(2);
      entity.setSplitDenominator(1);
      entity.setExecutionSymbol("AAPL");
      entity.setExecutionPriceAmount(BigDecimal.valueOf(100));
      entity.setExecutionPriceCurrency("USD");
      entity.setExecutionQuantity(BigDecimal.valueOf(10));
      entity.setNotes("NOTES");

      Transaction domain = mapper.toDomain(entity);

      assertThat(domain.split()).isNotNull();
      assertThat(domain.split().numerator()).isEqualTo(2);
      assertThat(domain.split().denominator()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("toEntity - Transaction to JPA")
  class ToEntityMapping {

    @Test
    @DisplayName("should convert domain BUY to fresh JPA entity")
    void shouldConvertBuyToEntity() {

      Transaction domain = createDomainBuyTransaction();

      TransactionJpaEntity entity = mapper.toEntity(domain, PORTFOLIO_UUID, IDEM_KEY);

      assertThat(entity.getId()).isEqualTo(TX_UUID);
      assertThat(entity.getPortfolioId()).isEqualTo(PORTFOLIO_UUID);
      assertThat(entity.getIdempotencyKey()).isEqualTo(IDEM_KEY);
      assertThat(entity.getExecutionSymbol()).isEqualTo("MSFT");
      assertThat(entity.getFees()).hasSize(1);
      assertThat(entity.getFees().get(0).getNativeAmount()).isEqualByComparingTo("2.50");
    }
  }

  @Nested
  @DisplayName("Exclusion Mutations")
  class ExclusionMutations {

    @Test
    @DisplayName("applyExclusionState should update managed entity fields directly")
    void applyExclusionStateShouldUpdateManagedEntity() {

      TransactionJpaEntity managed = createBaseJpaEntity();
      managed.setExcluded(false);

      Transaction domain = Transaction.builder().accountId(AccountId.newId())
          .transactionType(TransactionType.BUY).cashDelta(Money.of(-1000, Currency.CAD))
          .notes("NOTES").execution(new TradeExecution(new AssetSymbol("AAPL"), Quantity.of(10),
              Price.of("100", Currency.CAD)))
          .transactionId(TransactionId.fromString(TX_UUID.toString())).metadata(
              new TransactionMetadata(AssetType.STOCK, "MANUAL",
                  new TransactionMetadata.ExclusionRecord(Instant.now(),
                      UserId.fromString(USER_UUID.toString()), "Reversed"), null)).build();

      mapper.applyExclusionState(domain, managed);

      assertThat(managed.isExcluded()).isTrue();
      assertThat(managed.getExcludedReason()).isEqualTo("Reversed");
      assertThat(managed.getExcludedBy()).isEqualTo(USER_UUID);
    }

    @Test
    @DisplayName("applyExclusionState should clear exclusion when record is null")
    void applyExclusionStateShouldClearExclusion() {

      TransactionJpaEntity managed = createBaseJpaEntity();
      managed.setExcluded(true);
      managed.setExcludedReason("Old Reason");

      Transaction domain = Transaction.builder().accountId(AccountId.newId())
          .transactionType(TransactionType.BUY).cashDelta(Money.of(-1000, Currency.CAD))
          .notes("NOTES").execution(new TradeExecution(new AssetSymbol("AAPL"), Quantity.of(10),
              Price.of("100", Currency.CAD)))
          .transactionId(TransactionId.fromString(TX_UUID.toString()))
          .metadata(new TransactionMetadata(AssetType.STOCK, "MANUAL", null, null)).build();

      mapper.applyExclusionState(domain, managed);

      assertThat(managed.isExcluded()).isFalse();
      assertThat(managed.getExcludedReason()).isNull();
      assertThat(managed.getExcludedBy()).isNull();
    }
  }
}