package com.laderrco.fortunelink.portfolio.application.services.redislock;

public interface DistributedLockProvider {
  DistributedLock getLock(String key);
}