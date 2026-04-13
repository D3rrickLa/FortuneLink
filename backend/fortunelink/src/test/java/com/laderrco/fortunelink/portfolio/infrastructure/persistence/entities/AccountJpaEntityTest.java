package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("AccountJpaEntity Unit Tests")
class AccountJpaEntityTest {

  @Test
  @DisplayName("replacePositions should update existing, add new, and remove missing positions")
  void replacePositionsShouldSyncCollections() {
    
    AccountJpaEntity account = new AccountJpaEntity();
    UUID existingId = UUID.randomUUID();
    UUID newId = UUID.randomUUID();

    
    PositionJpaEntity existingPos = spy(PositionJpaEntity.create(
        existingId, account, "MARKET", "AAPL", "STOCK",
        BigDecimal.TEN, BigDecimal.valueOf(100), "USD", Instant.now(), Instant.now()));
    
    account.replacePositions(Set.of(existingPos));

    
    
    
    PositionJpaEntity updatedAAPL = PositionJpaEntity.create(
        existingId, null, "MARKET", "AAPL", "STOCK",
        BigDecimal.valueOf(20), BigDecimal.valueOf(200), "USD", Instant.now(), Instant.now());
    PositionJpaEntity newMSFT = PositionJpaEntity.create(
        newId, null, "MARKET", "MSFT", "STOCK",
        BigDecimal.ONE, BigDecimal.valueOf(300), "USD", Instant.now(), Instant.now());

    
    account.replacePositions(Set.of(updatedAAPL, newMSFT));

    
    Set<PositionJpaEntity> result = account.getPositions();
    assertThat(result).hasSize(2);

    
    
    
    verify(existingPos).applyFrom(updatedAAPL);
    assertThat(result).contains(existingPos);

    
    PositionJpaEntity msftResult = result.stream()
        .filter(p -> p.getId().equals(newId))
        .findFirst()
        .orElseThrow();
    assertThat(msftResult.getAccount()).isEqualTo(account);

    
    
    
  }

  @Test
  @DisplayName("addNewRealizedGains should append only and set back-reference")
  void addNewRealizedGainsShouldAppend() {
    
    AccountJpaEntity account = new AccountJpaEntity();
    RealizedGainJpaEntity gain = mock(RealizedGainJpaEntity.class);

    
    account.addNewRealizedGains(java.util.List.of(gain));

    
    assertThat(account.getRealizedGains()).contains(gain);
    verify(gain).setAccount(account);
  }

  @Test
  @DisplayName("isNew should reflect persistence state via lifecycle hooks")
  void isNewShouldToggleAfterLoad() {
    AccountJpaEntity account = new AccountJpaEntity();
    assertThat(account.isNew()).isTrue();

    
    account.markNotNew();
    assertThat(account.isNew()).isFalse();
  }
}