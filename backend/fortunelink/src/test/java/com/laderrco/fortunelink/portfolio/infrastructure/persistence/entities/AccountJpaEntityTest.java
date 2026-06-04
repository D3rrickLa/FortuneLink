package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("AccountJpaEntity Unit Tests")
class AccountJpaEntityTest {

  @Test
  @DisplayName("replacePositions should update existing, add new, and remove missing positions")
  void replacePositionsShouldSyncCollections() {
    AccountJpaEntity account = new AccountJpaEntity();
    UUID existingId = UUID.randomUUID();
    UUID newId = UUID.randomUUID();

    PositionJpaEntity existingPos = spy(
        PositionJpaEntity.create(existingId, account, "MARKET", "AAPL", "STOCK", BigDecimal.TEN,
            BigDecimal.valueOf(100), "USD", Instant.now(), Instant.now()));

    account.replacePositions(Set.of(existingPos));

    PositionJpaEntity updatedAAPL = PositionJpaEntity.create(existingId, null, "MARKET", "AAPL",
        "STOCK", BigDecimal.valueOf(20), BigDecimal.valueOf(200), "USD", Instant.now(),
        Instant.now());
    PositionJpaEntity newMSFT = PositionJpaEntity.create(newId, null, "MARKET", "MSFT", "STOCK",
        BigDecimal.ONE, BigDecimal.valueOf(300), "USD", Instant.now(), Instant.now());

    account.replacePositions(Set.of(updatedAAPL, newMSFT));

    Set<PositionJpaEntity> result = account.getPositions();
    assertThat(result).hasSize(2);

    verify(existingPos).applyFrom(updatedAAPL);
    assertThat(result).contains(existingPos);

    PositionJpaEntity msftResult = result.stream().filter(p -> p.getId().equals(newId)).findFirst()
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

  @Test
  @DisplayName("applyFrom: copies all scalar properties but does not touch collection associations")
  void applyFromCopiesScalarsAndSafeguardsCollections() {
    // Arrange - Target instance to be modified
    AccountJpaEntity target = new AccountJpaEntity();
    ReflectionTestUtils.setField(target, "name", "Old Checking Name");
    ReflectionTestUtils.setField(target, "accountType", "CHECKING");
    ReflectionTestUtils.setField(target, "positionStrategy", "STATIC");
    ReflectionTestUtils.setField(target, "healthStatus", "HEALTHY");
    ReflectionTestUtils.setField(target, "lifecycleState", "ACTIVE");
    ReflectionTestUtils.setField(target, "cashBalanceAmount", new BigDecimal("100.00"));
    ReflectionTestUtils.setField(target, "cashBalanceCurrency", "USD");
    ReflectionTestUtils.setField(target, "closedDate", null);
    ReflectionTestUtils.setField(target, "lastUpdatedOn", Instant.parse("2026-01-01T00:00:00Z"));

    // 1. Create standard mutable collections for the target
    Set<PositionJpaEntity> targetPositionsField = new java.util.LinkedHashSet<>();
    Set<RealizedGainJpaEntity> targetGainsField = new java.util.LinkedHashSet<>();

    PositionJpaEntity targetPosition = new PositionJpaEntity();
    RealizedGainJpaEntity targetGain = new RealizedGainJpaEntity();
    targetPositionsField.add(targetPosition);
    targetGainsField.add(targetGain);

    // 2. Inject them directly into the private fields, bypassing the defensive
    // getters
    ReflectionTestUtils.setField(target, "positions", targetPositionsField);
    ReflectionTestUtils.setField(target, "realizedGains", targetGainsField);

    // Arrange - Source data carrier
    AccountJpaEntity source = new AccountJpaEntity();
    ReflectionTestUtils.setField(source, "name", "New Investment Name");
    ReflectionTestUtils.setField(source, "accountType", "BROKERAGE");
    ReflectionTestUtils.setField(source, "positionStrategy", "DYNAMIC");
    ReflectionTestUtils.setField(source, "healthStatus", "CRITICAL");
    ReflectionTestUtils.setField(source, "lifecycleState", "SUSPENDED");
    ReflectionTestUtils.setField(source, "cashBalanceAmount", new BigDecimal("42000.50"));
    ReflectionTestUtils.setField(source, "cashBalanceCurrency", "CAD");
    ReflectionTestUtils.setField(source, "closedDate", Instant.parse("2026-05-28T12:00:00Z"));
    ReflectionTestUtils.setField(source, "lastUpdatedOn", Instant.parse("2026-05-28T18:00:00Z"));

    // 3. Do the same for the source's positions so source.getPositions() doesn't
    // blow up if it's read by anything
    Set<PositionJpaEntity> sourcePositionsField = new java.util.LinkedHashSet<>();
    PositionJpaEntity sourcePosition = new PositionJpaEntity();
    sourcePositionsField.add(sourcePosition);
    ReflectionTestUtils.setField(source, "positions", sourcePositionsField);

    // Act
    target.applyFrom(source);

    // Assert - Verify State Mutations
    assertThat(target.getName()).isEqualTo("New Investment Name");
    assertThat(target.getAccountType()).isEqualTo("BROKERAGE");
    assertThat(target.getPositionStrategy()).isEqualTo("DYNAMIC");
    assertThat(target.getHealthStatus()).isEqualTo("CRITICAL");
    assertThat(target.getLifecycleState()).isEqualTo("SUSPENDED");
    assertThat(target.getCashBalanceAmount()).isEqualByComparingTo("42000.50");
    assertThat(target.getCashBalanceCurrency()).isEqualTo("CAD");
    assertThat(target.getClosedDate()).isEqualTo("2026-05-28T12:00:00Z");
    assertThat(target.getLastUpdatedOn()).isEqualTo("2026-05-28T18:00:00Z");

    // Assert - Critical Regression Invariant Check (Collections left
    // uncopied/unmutated)
    assertThat(target.getPositions())
        .as("Positions must remain untouched so Hibernate can manage lifecycle tracking state")
        .containsExactly(targetPosition)
        .doesNotContain(sourcePosition);

    assertThat(target.getRealizedGains())
        .as("Realized gains collection must not be side-effected by applyFrom")
        .containsExactly(targetGain);
  }
}