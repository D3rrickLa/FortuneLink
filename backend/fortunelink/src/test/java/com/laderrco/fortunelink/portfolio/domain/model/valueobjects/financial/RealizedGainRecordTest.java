package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
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
    RealizedGainRecord record = new RealizedGainRecord(SYMBOL, RGL, CBS, now);
    assertThat(record.isGain()).isTrue();
    assertThat(record.isLoss()).isFalse();
  }

  @Test
  void testIsLoss() {
    Money RGL = Money.of(-30, USD_CURRENCY);
    Money CBS = Money.of(300, USD_CURRENCY);
    RealizedGainRecord record = new RealizedGainRecord(SYMBOL, RGL, CBS, now);
    assertThat(record.isGain()).isFalse();
    assertThat(record.isLoss()).isTrue();
  }
}
