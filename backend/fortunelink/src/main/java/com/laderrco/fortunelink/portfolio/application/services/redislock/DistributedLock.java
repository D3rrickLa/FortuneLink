package com.laderrco.fortunelink.portfolio.application.services.redislock;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {
  boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;
  void unlock();
}