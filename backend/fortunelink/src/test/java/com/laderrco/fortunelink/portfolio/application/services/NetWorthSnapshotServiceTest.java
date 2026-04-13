package com.laderrco.fortunelink.portfolio.application.services;

import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Net Worth Snapshot Service Unit Tests")
class NetWorthSnapshotServiceTest {

  @Mock
  private PortfolioRepository portfolioRepository;

  @Mock
  private UserSnapshotWorker worker;

  @InjectMocks
  private NetWorthSnapshotService snapshotService;

  @Nested
  @DisplayName("snapshotAllUsers Orchestration")
  class SnapshotOrchestration {

    @Test
    @DisplayName("snapshotAllUsers: processes batch and survives individual failures")
    void snapshotAllUsersProcessesBatch() {

      UserId u1 = UserId.random();
      UserId u2 = UserId.random();
      UserId u3 = UserId.random();

      when(portfolioRepository.findAllActiveUserIds()).thenReturn(List.of(u1, u2, u3));

      when(worker.snapshotForUser(u1)).thenReturn(true);

      when(worker.snapshotForUser(u2)).thenReturn(false);

      when(worker.snapshotForUser(u3)).thenThrow(new RuntimeException("Database error"));

      snapshotService.snapshotAllUsers();

      verify(worker, times(1)).snapshotForUser(u1);
      verify(worker, times(1)).snapshotForUser(u2);
      verify(worker, times(1)).snapshotForUser(u3);

      verify(portfolioRepository, times(1)).findAllActiveUserIds();
    }

    @Test
    @DisplayName("snapshotAllUsers: handles empty user list gracefully")
    void handlesEmptyList() {
      when(portfolioRepository.findAllActiveUserIds()).thenReturn(List.of());

      snapshotService.snapshotAllUsers();

      verifyNoInteractions(worker);
    }
  }
}