package com.laderrco.fortunelink.portfolio.application.services.redislock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("RedissonLockProvider Implementation Tests")
class RedissonLockProviderTest {

  @Mock
  private RedissonClient redissonClient;

  @InjectMocks
  private RedissonLockProvider lockProvider;

  private final String lockKey = "lock:portfolio:update";
  private final RLock mockRLock = mock(RLock.class);

  @BeforeEach
  void setUp() {
    // Ensure that every time your provider asks Redisson for a lock name,
    // it gets our mocked RLock backend
    when(redissonClient.getLock(lockKey)).thenReturn(mockRLock);
  }

  @Nested
  @DisplayName("When obtaining a lock instance")
  class GetLock {

    @Test
    @DisplayName("Should return a custom DistributedLock instance that wraps Redisson")
    void shouldReturnCustomLockWrapper() {
      // Act
      DistributedLock lock = lockProvider.getLock(lockKey);

      // Assert
      assertThat(lock).isNotNull();
      assertThat(lock).isInstanceOf(DistributedLock.class);
    }
  }

  @Nested
  @DisplayName("When exercising the custom DistributedLock proxy")
  class DistributedLockBehaviors {

    private DistributedLock customLock;

    @BeforeEach
    void initLockWrapper() {
      customLock = lockProvider.getLock(lockKey);
    }

    @Test
    @DisplayName("tryLock: Should accurately delegate timeout arguments down to Redisson's lock")
    void shouldForwardTryLockArguments() throws InterruptedException {
      // Arrange
      long waitTime = 2;
      long leaseTime = 5;
      TimeUnit unit = TimeUnit.SECONDS;

      when(mockRLock.tryLock(waitTime, leaseTime, unit)).thenReturn(true);

      // Act
      boolean result = customLock.tryLock(waitTime, leaseTime, unit);

      // Assert
      assertThat(result).isTrue();
      verify(mockRLock).tryLock(waitTime, leaseTime, unit);
    }

    @Test
    @DisplayName("unlock: Should forward the call directly to Redisson's unlock wrapper")
    void shouldForwardUnlockInvocation() {
      // Act
      customLock.unlock();

      // Assert
      verify(mockRLock).unlock();
    }
  }
}