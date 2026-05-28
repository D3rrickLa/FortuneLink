package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import com.laderrco.fortunelink.portfolio.application.views.ValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountValuationSnapshotTest {

  private final AccountId accountId = new AccountId(UUID.randomUUID());
  private final LocalDate today = LocalDate.now();
  private final Currency usd = Currency.of("USD");

  @Nested
  @DisplayName("Constructor Invariants")
  class ConstructorInvariants {

    @Test
    @DisplayName("Initialization throws NullPointerException when mandatory properties are missing")
    void rejectsNullProperties() {
      Money validMoney = Money.of("100.00", usd);

      assertThatThrownBy(() -> new AccountValuationSnapshot(null, today, validMoney, validMoney, validMoney,
          new PercentageChange(BigDecimal.ZERO), validMoney, validMoney, false))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Account ID cannot be null");

      assertThatThrownBy(() -> new AccountValuationSnapshot(accountId, null, validMoney, validMoney, validMoney,
          new PercentageChange(BigDecimal.ZERO), validMoney, validMoney, false))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Snapshot date cannot be null");
    }

    @Test
    @DisplayName("Initialization throws IllegalArgumentException if totalValue != cashBalance + investedValue")
    void enforcesMathematicalBalanceInvariant() {
      Money totalValue = Money.of("150.00", usd); // Incorrect total balance
      Money cashBalance = Money.of("50.00", usd);
      Money investedValue = Money.of("80.00", usd); // 50 + 80 = 130 != 150
      Money costBasis = Money.of("100.00", usd);
      Money unrealized = Money.of("50.00", usd);

      assertThatThrownBy(() -> new AccountValuationSnapshot(
          accountId, today, totalValue, costBasis, unrealized, new PercentageChange(BigDecimal.ZERO), cashBalance,
          investedValue, false))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Total value must equal cash balance plus invested value");
    }
  }

  @Nested
  @DisplayName("Static Factories")
  class StaticFactories {

    @Test
    @DisplayName("create: mathematically derives totalValue, unrealizedGL, and percentage changes accurately")
    void createDerivesValuesCorrectly() {
      // Arrange
      Money cash = Money.of("30.00", usd);
      Money invested = Money.of("70.00", usd);
      Money costBasis = Money.of("80.00", usd);

      // Act
      AccountValuationSnapshot snapshot = AccountValuationSnapshot.create(accountId, cash, invested, costBasis);

      // Assert
      assertThat(snapshot.accountId()).isEqualTo(accountId);
      assertThat(snapshot.snapshotDate()).isEqualTo(LocalDate.now());

      // 30.00 + 70.00 = 100.00
      assertThat(snapshot.totalValue()).isEqualTo(Money.of("100.00", usd));
      // 100.00 - 80.00 = 20.00
      assertThat(snapshot.unrealizedGainLoss()).isEqualTo(Money.of("20.00", usd));

      assertThat(snapshot.cashBalance()).isEqualTo(cash);
      assertThat(snapshot.investedValue()).isEqualTo(invested);
      assertThat(snapshot.isStale()).isFalse();
    }

    @Test
    @DisplayName("fromView: maps structural properties directly out of a ValuationView object context")
    void fromViewMapsPropertiesCleanly() {
      // Arrange
      Instant fixedInstant = Instant.parse("2026-05-28T14:30:00Z");
      LocalDate expectedDate = LocalDate.ofInstant(fixedInstant, ZoneOffset.UTC);

      ValuationView view = new ValuationView(
          Money.of("500.00", usd), // totalValue
          Money.of("400.00", usd), // totalCostBasis
          Money.of("100.00", usd), // unrealizedGainLoss
          new BigDecimal("25.00"), // gainLossPercent
          Money.of("150.00", usd), // totalCashBalance
          Money.of("350.00", usd), // totalInvestedValue (150 + 350 = 500)
          usd, // displayCurrency
          true, // hasStaleData
          fixedInstant // asOfDate
      );

      // Act
      AccountValuationSnapshot snapshot = AccountValuationSnapshot.fromView(accountId, view);

      // Assert
      assertThat(snapshot.accountId()).isEqualTo(accountId);
      assertThat(snapshot.snapshotDate()).isEqualTo(expectedDate);
      assertThat(snapshot.totalValue()).isEqualTo(view.totalValue());
      assertThat(snapshot.totalCostBasis()).isEqualTo(view.totalCostBasis());
      assertThat(snapshot.unrealizedGainLoss()).isEqualTo(view.unrealizedGainLoss());
      assertThat(snapshot.gainLossPercent()).isEqualTo(new PercentageChange(view.gainLossPercent()));
      assertThat(snapshot.cashBalance()).isEqualTo(view.totalCashBalance());
      assertThat(snapshot.investedValue()).isEqualTo(view.totalInvestedValue());
      assertThat(snapshot.isStale()).isTrue();
    }
  }
}