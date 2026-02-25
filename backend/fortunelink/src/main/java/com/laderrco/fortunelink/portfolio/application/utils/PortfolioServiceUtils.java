package com.laderrco.fortunelink.portfolio.application.utils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public class PortfolioServiceUtils {
    public static Set<AssetSymbol> extractSymbols(Portfolio portfolio) {
        return portfolio.getAccounts().stream()
                .flatMap(account -> account.getPositionEntries().stream().map(Map.Entry::getKey))
                .collect(Collectors.toSet());
    }

    public static Set<AssetSymbol> extractSymbolsByAccount(Account account) {
        return account.getPositionEntries().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
