package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RealizedGainRecordTest {
  private static final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private static final Currency USD_CURRENCY = Currency.USD;
  private Instant now;

  @BeforeEach
  void setup() {
    now = Instant.now();
  }

  @Test
  void testIsGain() {
    Money RGL = Money.of(30, USD_CURRENCY);
    Money CBS = Money.of(300, USD_CURRENCY);
    RealizedGainRecord record = RealizedGainRecord.of(SYMBOL, RGL, CBS, now);
    assertThat(record.isGain()).isTrue();
    assertThat(record.isLoss()).isFalse();
  }

  @Test
  void testIsLoss() {
    Money RGL = Money.of(-30, USD_CURRENCY);
    Money CBS = Money.of(300, USD_CURRENCY);
    RealizedGainRecord record = RealizedGainRecord.of(SYMBOL, RGL, CBS, now);
    assertThat(record.isGain()).isFalse();
    assertThat(record.isLoss()).isTrue();
  }

  @Test
  void testReconstitute() {
    RealizedGainRecord record = RealizedGainRecord.reconstitute(UUID.randomUUID(), SYMBOL,
        Money.zero(USD_CURRENCY), Money.zero(USD_CURRENCY), now);

    assertThat(record.symbol()).isEqualTo(SYMBOL);
    assertThat(record.occurredAt()).isEqualTo(now);
  }
}
