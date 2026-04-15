package com.laderrco.fortunelink.portfolio.application.services.redislock;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedissonLockProvider implements DistributedLockProvider {
  private final RedissonClient redisson;

  @Override
  public DistributedLock getLock(String key) {
    RLock rLock = redisson.getLock(key);

    return new DistributedLock() {
      @Override
      public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit)
          throws InterruptedException {
        return rLock.tryLock(waitTime, leaseTime, unit);
      }

      @Override
      public void unlock() {
        rLock.unlock();
      }
    };
  }
}
