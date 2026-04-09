package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.application.repositories.AccountQueryRepository;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.services.projectors.AssetBalanceProjection;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers.PortfolioDomainMapper;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSummaryProjection;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.projections.AccountSymbolProjection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountQueryRepositoryImpl implements AccountQueryRepository {
  private final JpaAccountRepository jpaAccountRepository;
  private final PortfolioDomainMapper mapper;

  @Override
  public Page<AccountSummaryProjection> findByPortfolioId(PortfolioId portfolioId,
      Pageable pageable) {
    return jpaAccountRepository.findByPortfolioId(UUID.fromString(portfolioId.toString()),
        pageable);
  }

  /**
   * Groups (accountId, symbol) rows into Map<AccountId, Set<AssetSymbol>>.
   * Accounts with no open
   * positions are absent from the result — callers should use getOrDefault(id,
   * Set.of()).
   */
  @Override
  public Map<AccountId, Set<AssetSymbol>> findSymbolsForAccounts(List<AccountId> accountIds) {
    if (accountIds == null || accountIds.isEmpty()) {
      return Map.of();
    }

    List<UUID> uuids = accountIds.stream().map(id -> UUID.fromString(id.toString())).toList();

    List<AccountSymbolProjection> rows = jpaAccountRepository.findSymbolsForAccounts(uuids);

    // Group by accountId, collect symbols into a Set per account.
    Map<AccountId, Set<AssetSymbol>> result = new LinkedHashMap<>();
    for (AccountSymbolProjection row : rows) {
      AccountId accountId = AccountId.fromString(row.getAccountId().toString());
      result.computeIfAbsent(accountId, k -> new LinkedHashSet<>())
          .add(new AssetSymbol(row.getSymbol()));
    }

    return Collections.unmodifiableMap(result);
  }

  @Override
  public Optional<Account> findByIdWithDetails(AccountId accountId, PortfolioId portfolioId,
      UserId userId) {
    return jpaAccountRepository.findByIdWithOwnershipCheck(UUID.fromString(accountId.toString()),
        UUID.fromString(portfolioId.toString()), UUID.fromString(userId.toString()))
        .map(mapper::accountToDomain);
  }

  @Override
  public Map<AccountId, Map<AssetSymbol, Quantity>> findQuantitiesForAccounts(List<AccountId> accountIds) {
    if (accountIds == null || accountIds.isEmpty()) {
      return Map.of();
    }

    List<UUID> uuids = accountIds.stream()
        .map(id -> UUID.fromString(id.toString()))
        .toList();

    List<AssetBalanceProjection> rows = jpaAccountRepository.findBalancesForAccounts(uuids);

    Map<AccountId, Map<AssetSymbol, Quantity>> result = new LinkedHashMap<>();
    for (AssetBalanceProjection row : rows) {
      AccountId accountId = AccountId.fromString(row.getAccountId().toString());
      AssetSymbol symbol = new AssetSymbol(row.getSymbol());
      Quantity qty = new Quantity(row.getQuantity());
      result.computeIfAbsent(accountId, k -> new LinkedHashMap<>()).put(symbol, qty);
    }
    return Collections.unmodifiableMap(result);
  }
}