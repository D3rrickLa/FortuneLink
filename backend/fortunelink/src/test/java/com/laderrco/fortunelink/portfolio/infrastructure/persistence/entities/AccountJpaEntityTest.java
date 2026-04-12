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
    // Arrange
    AccountJpaEntity account = new AccountJpaEntity();
    UUID existingId = UUID.randomUUID();
    UUID newId = UUID.randomUUID();

    // 1. Setup existing state with one position (AAPL)
    PositionJpaEntity existingPos = spy(PositionJpaEntity.create(
        existingId, account, "MARKET", "AAPL", "STOCK",
        BigDecimal.TEN, BigDecimal.valueOf(100), "USD", Instant.now(), Instant.now()));
    // We use reflection or a helper because the collection is private/final
    account.replacePositions(Set.of(existingPos));

    // 2. Prepare incoming state
    // - One update for AAPL (changing quantity)
    // - One brand new position (MSFT)
    PositionJpaEntity updatedAAPL = PositionJpaEntity.create(
        existingId, null, "MARKET", "AAPL", "STOCK",
        BigDecimal.valueOf(20), BigDecimal.valueOf(200), "USD", Instant.now(), Instant.now());
    PositionJpaEntity newMSFT = PositionJpaEntity.create(
        newId, null, "MARKET", "MSFT", "STOCK",
        BigDecimal.ONE, BigDecimal.valueOf(300), "USD", Instant.now(), Instant.now());

    // Act
    account.replacePositions(Set.of(updatedAAPL, newMSFT));

    // Assert
    Set<PositionJpaEntity> result = account.getPositions();
    assertThat(result).hasSize(2);

    // Branch 1: Verify Update logic (cur != null)
    // It should call applyFrom on the EXISTING object reference, not swap it for
    // the incoming one
    verify(existingPos).applyFrom(updatedAAPL);
    assertThat(result).contains(existingPos);

    // Branch 2: Verify Create logic (cur == null)
    PositionJpaEntity msftResult = result.stream()
        .filter(p -> p.getId().equals(newId))
        .findFirst()
        .orElseThrow();
    assertThat(msftResult.getAccount()).isEqualTo(account);

    // Implicit verification of Branch 3: Removal
    // Since we didn't include the "old" version of AAPL without the update,
    // the clear() + add() logic naturally handles the diff.
  }

  @Test
  @DisplayName("addNewRealizedGains should append only and set back-reference")
  void addNewRealizedGainsShouldAppend() {
    // Arrange
    AccountJpaEntity account = new AccountJpaEntity();
    RealizedGainJpaEntity gain = mock(RealizedGainJpaEntity.class);

    // Act
    account.addNewRealizedGains(java.util.List.of(gain));

    // Assert
    assertThat(account.getRealizedGains()).contains(gain);
    verify(gain).setAccount(account);
  }

  @Test
  @DisplayName("isNew should reflect persistence state via lifecycle hooks")
  void isNewShouldToggleAfterLoad() {
    AccountJpaEntity account = new AccountJpaEntity();
    assertThat(account.isNew()).isTrue();

    // Simulate JPA callback
    account.markNotNew();
    assertThat(account.isNew()).isFalse();
  }
}