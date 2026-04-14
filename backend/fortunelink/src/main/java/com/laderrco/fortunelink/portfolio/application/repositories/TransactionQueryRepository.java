package com.laderrco.fortunelink.portfolio.application.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionQueryRepository {
  Page<Transaction> findTransactionsDynamic(AccountId accountId, AssetSymbol symbol, Instant start,
      Instant end, Pageable pageable);
}