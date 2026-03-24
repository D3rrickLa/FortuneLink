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
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'save'");
  }

  @Override
  public int deleteExpiredTransactions(AccountId accountId, Instant cutoff) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteExpiredTransactions'");
  }

  @Override
  public int deleteAllExpiredTransactions(Instant cutoff) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deleteAllExpiredTransactions'");
  }

  @Override
  public List<Transaction> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findByAccountIdAndSymbol'");
  }

  @Override
  public List<Transaction> findByAccountIdAndDateRange(AccountId accountId, Instant start, Instant end) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findByAccountIdAndDateRange'");
  }

  @Override
  public Optional<Transaction> findByIdAndPortfolioIdAndUserIdAndAccountId(TransactionId id, PortfolioId portfolioId,
      UserId userId, AccountId accountId) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findByIdAndPortfolioIdAndUserIdAndAccountId'");
  }

  @Override
  public Map<AccountId, Map<AssetSymbol, Money>> sumBuyFeesByAccountAndSymbol(List<AccountId> accountIds) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'sumBuyFeesByAccountAndSymbol'");
  }

}
