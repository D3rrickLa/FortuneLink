package com.laderrco.fortunelink.portfolio.infrastructure.config.cachedidempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaffeineIdempotencyCacheTest {

  private CaffeineIdempotencyCache idempotencyCache;

  @BeforeEach
  void setUp() {
    idempotencyCache = new CaffeineIdempotencyCache();
  }

  @Test
  void shouldStoreAndRetrieveValue() {

    String key = "idempotency-key-" + UUID.randomUUID();
    TransactionView view = new TransactionView(TransactionId.newId(), TransactionType.BUY, "AAPL",
        Quantity.of(10), Price.of("100", Currency.CAD), List.of(), Money.of(1000, Currency.CAD),
        Map.of(), Instant.now(), "notes");

    idempotencyCache.put(key, view);
    TransactionView retrieved = idempotencyCache.get(key);

    assertThat(retrieved).isNotNull();
    assertThat(retrieved).isEqualTo(view);
  }

  @Test
  void shouldReturnNullWhenKeyDoesNotExist() {
    TransactionView retrieved = idempotencyCache.get("non-existent-key");

    assertThat(retrieved).isNull();
  }

  @Test
  void shouldOverwriteExistingValue() {
    String key = "shared-key";
    TransactionView firstView = new TransactionView(TransactionId.newId(), TransactionType.BUY,
        "AAPL", Quantity.of(10), Price.of("100", Currency.CAD), List.of(),
        Money.of(1000, Currency.CAD), Map.of(), Instant.now(), "notes");
    TransactionView secondView = new TransactionView(TransactionId.newId(), TransactionType.BUY,
        "AAPL", Quantity.of(12), Price.of("100", Currency.CAD), List.of(),
        Money.of(1000, Currency.CAD), Map.of(), Instant.now(), "notes");

    idempotencyCache.put(key, firstView);
    idempotencyCache.put(key, secondView);
    TransactionView retrieved = idempotencyCache.get(key);

    assertThat(retrieved).isEqualTo(secondView);
    assertThat(retrieved).isNotEqualTo(firstView);
  }
}