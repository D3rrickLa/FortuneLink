package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.NetWorthSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.NetWorthSnapshotJpaEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("NetWorthSnapshotRepositoryImpl Unit Tests")
class NetWorthSnapshotRepositoryImplTest {

  @Mock
  private JpaNetWorthSnapshotRepository jpaRepository;

  @InjectMocks
  private NetWorthSnapshotRepositoryImpl repository;

  private static final UUID USER_UUID = UUID.randomUUID();
  private static final UserId USER_ID = UserId.fromString(USER_UUID.toString());
  private static final Currency CAD = Currency.CAD;

  @Nested
  @DisplayName("Persistence Operations")
  class PersistenceOperations {

    @Test
    @DisplayName("save should delegate to jpaRepository after mapping")
    void saveShouldDelegateToJpaRepository() {
      NetWorthSnapshot snapshot = new NetWorthSnapshot(
          UUID.randomUUID(),
          USER_ID,
          Money.of(100, CAD),
          Money.of(0, CAD),
          Money.of(100, CAD),
          CAD,
          false,
          Instant.now());

      repository.save(snapshot);

      verify(jpaRepository).save(any(NetWorthSnapshotJpaEntity.class));
    }

    @Test
    @DisplayName("findByUserIdSince should return mapped domain objects")
    void findByUserIdSinceShouldReturnMappedDomainObjects() {
      Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
      NetWorthSnapshotJpaEntity entity = createMockEntity();
      when(jpaRepository.findByUserIdSince(USER_UUID, since)).thenReturn(List.of(entity));

      List<NetWorthSnapshot> results = repository.findByUserIdSince(USER_ID, since);

      assertThat(results).hasSize(1);
      verify(jpaRepository).findByUserIdSince(USER_UUID, since);
    }
  }

  @Nested
  @DisplayName("Date Boundary Logic")
  class DateBoundaryLogic {

    @Test
    @DisplayName("existsForToday should check between start and end of UTC day")
    void existsForTodayShouldCheckBetweenStartAndEndOfUtcDay() {
      // Expected boundaries
      Instant expectedStart = LocalDate.now(ZoneOffset.UTC)
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant();
      Instant expectedEnd = expectedStart.plus(1, ChronoUnit.DAYS);

      when(jpaRepository.existsBetween(eq(USER_UUID), any(Instant.class), any(Instant.class)))
          .thenReturn(true);

      boolean exists = repository.existsForToday(USER_ID);

      assertThat(exists).isTrue();
      verify(jpaRepository).existsBetween(USER_UUID, expectedStart, expectedEnd);
    }
  }

  // --- Helper Methods ---

  private NetWorthSnapshotJpaEntity createMockEntity() {
    // This assumes your JpaEntity has a toDomain method that won't NPE on a blank
    // entity if it does, use a proper builder or setter here.
    NetWorthSnapshotJpaEntity entity = NetWorthSnapshotJpaEntity.from(
        new NetWorthSnapshot(
            UUID.randomUUID(),
            USER_ID,
            Money.of(100, CAD),
            Money.of(0, CAD),
            Money.of(100, CAD),
            CAD,
            false,
            Instant.now()));
    // Set necessary fields if toDomain() requires them
    return entity;
  }
}