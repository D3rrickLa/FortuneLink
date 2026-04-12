package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("PositionJpaEntity Unit Tests")
class PositionJpaEntityTest {

  @Test
  @DisplayName("create() should correctly initialize all fields")
  void createShouldInitializeFields() {
    // Arrange
    UUID id = UUID.randomUUID();
    AccountJpaEntity account = mock(AccountJpaEntity.class);
    Instant now = Instant.now();

    // Act
    PositionJpaEntity entity = PositionJpaEntity.create(
        id, account, "MARKET", "AAPL", "STOCK",
        new BigDecimal("10.5"), new BigDecimal("1500.00"),
        "USD", now, now);

    // Assert
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getAccount()).isEqualTo(account);
    assertThat(entity.getIdentifierType()).isEqualTo("MARKET");
    assertThat(entity.getSymbol()).isEqualTo("AAPL");
    assertThat(entity.getAssetType()).isEqualTo("STOCK");
    assertThat(entity.getQuantity()).isEqualByComparingTo("10.5");
    assertThat(entity.getCostBasisAmount()).isEqualByComparingTo("1500.00");
    assertThat(entity.getCostBasisCurrency()).isEqualTo("USD");
    assertThat(entity.getAcquiredDate()).isEqualTo(now);
    assertThat(entity.getLastModifiedAt()).isEqualTo(now);
  }

  @Test
  @DisplayName("applyFrom() should update mutable fields but ignore identity (id/symbol/account)")
  void applyFromShouldUpdateFields() {
    // Arrange
    PositionJpaEntity target = PositionJpaEntity.create(
        UUID.randomUUID(), mock(AccountJpaEntity.class), "MARKET", "AAPL", "STOCK",
        BigDecimal.ONE, BigDecimal.TEN, "USD", Instant.MIN, Instant.MIN);

    Instant newTime = Instant.now();
    PositionJpaEntity source = PositionJpaEntity.create(
        UUID.randomUUID(), // Different ID
        mock(AccountJpaEntity.class), // Different Account
        "CRYPTO", "BTC", // Different symbol/type
        "CRYPTO_ASSET",
        new BigDecimal("2.5"), new BigDecimal("50000.00"),
        "BTC", newTime, newTime);

    // Act
    target.applyFrom(source);

    // Assert
    // Identity/Key fields should NOT change (based on typical DDD/JPA patterns)
    assertThat(target.getSymbol()).isEqualTo("AAPL");

    // Value fields SHOULD change
    assertThat(target.getIdentifierType()).isEqualTo("CRYPTO");
    assertThat(target.getAssetType()).isEqualTo("CRYPTO_ASSET");
    assertThat(target.getQuantity()).isEqualByComparingTo("2.5");
    assertThat(target.getCostBasisAmount()).isEqualByComparingTo("50000.00");
    assertThat(target.getCostBasisCurrency()).isEqualTo("BTC");
    assertThat(target.getAcquiredDate()).isEqualTo(newTime);
    assertThat(target.getLastModifiedAt()).isEqualTo(newTime);
  }

  @Test
  @DisplayName("setAccount() should update the account reference")
  void setAccountShouldUpdateReference() {
    // Arrange
    PositionJpaEntity entity = new PositionJpaEntity();
    AccountJpaEntity newAccount = mock(AccountJpaEntity.class);

    // Act
    entity.setAccount(newAccount);

    // Assert
    assertThat(entity.getAccount()).isEqualTo(newAccount);
  }
}