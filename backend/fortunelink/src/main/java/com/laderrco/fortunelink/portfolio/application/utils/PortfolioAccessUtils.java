package com.laderrco.fortunelink.portfolio.application.utils;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PortfolioAccessUtils {
  private PortfolioAccessUtils() {

  }

  public static Set<AssetSymbol> extractSymbols(Portfolio portfolio) {
    return portfolio.getAccounts().stream()
        .flatMap(account -> account.getPositionEntries().stream().map(Map.Entry::getKey))
        .collect(Collectors.toSet());
  }

  public static Set<AssetSymbol> extractSymbolsFromAccounts(List<Account> accounts) {
    return accounts.stream()
        // Access the entries of the position map for each account
        .flatMap(account -> account.getPositionEntries().stream())
        // Extract the key (AssetSymbol) from the Map.Entry
        .map(Map.Entry::getKey)
        // Collect into a unique set
        .collect(Collectors.toSet());
  }

  public static Set<AssetSymbol> extractSymbolsByAccount(Account account) {
    return account.getPositionEntries().stream().map(Map.Entry::getKey).collect(Collectors.toSet());
  }
}
