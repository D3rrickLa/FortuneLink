package com.laderrco.fortunelink.portfolio.infrastructure.config.cachedidempotency;

import com.laderrco.fortunelink.portfolio.application.views.TransactionView;

public interface IdempotencyCache {
  TransactionView get(String key);
  void put(String key, TransactionView value);
}
