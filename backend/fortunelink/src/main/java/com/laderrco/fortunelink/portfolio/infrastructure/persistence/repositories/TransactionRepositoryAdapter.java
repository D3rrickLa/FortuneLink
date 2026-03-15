package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TransactionRepositoryAdapter implements TransactionRepository {

  @Override
  public Transaction save(Transaction transaction) {
    return null;
  }

  @Override
  public int deleteExpiredTransactions(AccountId accountId, Instant cutoff) {
    return 0;
  }

  @Override
  public int deleteAllExpiredTransactions(Instant cutoff) {
    return 0;
  }

  @Override
  public List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol) {
    return List.of();
  }

  @Override
  public Optional<Transaction> findByIdAndPortfolioIdAndUserIdAndAccountId(TransactionId id,
      PortfolioId portfolioId, UserId userId, AccountId accountId) {
    return Optional.empty();
  }

  @Override
  public Map<AccountId, Map<AssetSymbol, Money>> sumBuyFeesByAccountAndSymbol(
      List<AccountId> accountIds) {
    return Map.of();
  }
}
