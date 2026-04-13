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
    
    UUID id = UUID.randomUUID();
    AccountJpaEntity account = mock(AccountJpaEntity.class);
    Instant now = Instant.now();

    
    PositionJpaEntity entity = PositionJpaEntity.create(
        id, account, "MARKET", "AAPL", "STOCK",
        new BigDecimal("10.5"), new BigDecimal("1500.00"),
        "USD", now, now);

    
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
    
    PositionJpaEntity target = PositionJpaEntity.create(
        UUID.randomUUID(), mock(AccountJpaEntity.class), "MARKET", "AAPL", "STOCK",
        BigDecimal.ONE, BigDecimal.TEN, "USD", Instant.MIN, Instant.MIN);

    Instant newTime = Instant.now();
    PositionJpaEntity source = PositionJpaEntity.create(
        UUID.randomUUID(), 
        mock(AccountJpaEntity.class), 
        "CRYPTO", "BTC", 
        "CRYPTO_ASSET",
        new BigDecimal("2.5"), new BigDecimal("50000.00"),
        "BTC", newTime, newTime);

    
    target.applyFrom(source);

    
    
    assertThat(target.getSymbol()).isEqualTo("AAPL");

    
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
    
    PositionJpaEntity entity = new PositionJpaEntity();
    AccountJpaEntity newAccount = mock(AccountJpaEntity.class);

    
    entity.setAccount(newAccount);

    
    assertThat(entity.getAccount()).isEqualTo(newAccount);
  }
}