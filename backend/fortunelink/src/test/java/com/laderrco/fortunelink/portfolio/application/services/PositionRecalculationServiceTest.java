package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.utils.PositionRecalculationExecutor;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Position Recalculation Service Unit Tests")
public class PositionRecalculationServiceTest {
  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final AccountId ACCOUNT_ID = AccountId.newId();
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");

  @Mock
  private RedissonClient redissonClient;
  @Mock
  private PositionRecalculationExecutor executor;
  @InjectMocks
  private PositionRecalculationService recalculationService;

  private PositionRecalculationRequestedEvent createEvent() {
    return new PositionRecalculationRequestedEvent(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL);
  }

  @Nested
  @DisplayName("Creation and Basic Validation")
  class ValidationTests {
    @Test
    @DisplayName("onRecalculationRequested: throws exception when event is null")
    void onRecalculationRequestedThrowsOnNullEvent() {
      assertThatThrownBy(() -> recalculationService.onRecalculationRequested(null)).isInstanceOf(
          NullPointerException.class);
    }
  }

  @Nested
  @DisplayName("Execution and Locking")
  class ExecutionTests {
    @Test
    @DisplayName("onRecalculationRequested: successfully schedules recalculation")
    void onRecalculationRequestedSchedulesSuccessfully() {
      PositionRecalculationRequestedEvent event = createEvent();
      RLock mockLock = mock(RLock.class);

      when(redissonClient.getLock(anyString())).thenReturn(mockLock);
      try {
        when(mockLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      recalculationService.onRecalculationRequested(event);

      verify(executor).scheduleRecalculation(any(), any(), any(), any());
    }

    @Test
    @DisplayName("onRecalculationRequested: handles InterruptedException and restores interrupt status")
    void onRecalculationRequestedHandlesInterruptedException() throws InterruptedException {
      PositionRecalculationRequestedEvent event = createEvent();
      RLock mockLock = mock(RLock.class);

      when(redissonClient.getLock(anyString())).thenReturn(mockLock);
      when(mockLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
          .thenThrow(new InterruptedException("Simulation of interrupted thread"));

      RuntimeException exception = assertThrows(RuntimeException.class, () -> {
        recalculationService.onRecalculationRequested(event);
      });

      assertTrue(exception.getMessage().contains("Interrupted while waiting for position lock"));
      assertTrue(Thread.interrupted(), "The interrupt flag should be set on the current thread");

      verifyNoInteractions(executor);
    }
  }
}